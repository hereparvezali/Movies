use serde::{Deserialize, Serialize};
use std::env;
use std::fs;
use std::io::{self, Write};
use std::path::Path;
use std::time::{SystemTime, UNIX_EPOCH};

const DEFAULT_SERVICE_ACCOUNT_PATH: &str =
    r"C:\Users\parvez\Downloads\movies-bdjobs-firebase-adminsdk-fbsvc-f80342e98d.json";
const FCM_SCOPE: &str = "https://www.googleapis.com/auth/firebase.messaging";
const GOOGLE_TOKEN_URL: &str = "https://oauth2.googleapis.com/token";

#[derive(Debug, Deserialize)]
struct ServiceAccountKey {
    project_id: String,
    client_email: String,
    private_key: String,
    token_uri: Option<String>,
}

#[derive(Debug, Serialize)]
struct JwtClaims<'a> {
    iss: &'a str,
    scope: &'a str,
    aud: &'a str,
    iat: u64,
    exp: u64,
}

#[derive(Debug, Deserialize)]
struct OAuthTokenResponse {
    access_token: String,
}

async fn get_access_token(
    client: &reqwest::Client,
    sa: &ServiceAccountKey,
) -> Result<String, Box<dyn std::error::Error>> {
    let now = SystemTime::now().duration_since(UNIX_EPOCH)?.as_secs();

    let claims = JwtClaims {
        iss: &sa.client_email,
        scope: FCM_SCOPE,
        aud: sa.token_uri.as_deref().unwrap_or(GOOGLE_TOKEN_URL),
        iat: now,
        exp: now + 3600,
    };

    let key = jsonwebtoken::EncodingKey::from_rsa_pem(sa.private_key.as_bytes())
        .map_err(|e| format!("Failed to parse private key: {}", e))?;

    let header = jsonwebtoken::Header::new(jsonwebtoken::Algorithm::RS256);
    let jwt = jsonwebtoken::encode(&header, &claims, &key)
        .map_err(|e| format!("Failed to encode JWT: {}", e))?;

    let token_endpoint = sa.token_uri.as_deref().unwrap_or(GOOGLE_TOKEN_URL);
    let params = [
        ("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer"),
        ("assertion", jwt.as_str()),
    ];

    let resp = client
        .post(token_endpoint)
        .form(&params)
        .send()
        .await
        .map_err(|e| format!("Failed to request OAuth token: {}", e))?;

    if !resp.status().is_success() {
        let err_text = resp.text().await.unwrap_or_default();
        return Err(format!("Google OAuth token request failed: {}", err_text).into());
    }

    let token_data: OAuthTokenResponse = resp.json().await?;
    Ok(token_data.access_token)
}

async fn send_fcm_movie_message(
    client: &reqwest::Client,
    access_token: &str,
    project_id: &str,
    device_token: &str,
    movie_id: &str,
    title: &str,
    body: &str,
) -> Result<String, Box<dyn std::error::Error>> {
    let url = format!(
        "https://fcm.googleapis.com/v1/projects/{}/messages:send",
        project_id
    );

    let payload = serde_json::json!({
        "message": {
            "token": device_token,
            "notification": {
                "title": title,
                "body": body
            },
            "data": {
                "movie_id": movie_id,
                "title": title,
                "body": body
            }
        }
    });

    let resp = client
        .post(&url)
        .bearer_auth(access_token)
        .json(&payload)
        .send()
        .await
        .map_err(|e| format!("Failed to send FCM request: {}", e))?;

    let status = resp.status();
    let body_text = resp.text().await.unwrap_or_default();

    if status.is_success() {
        Ok(body_text)
    } else {
        Err(format!("FCM request failed with status {}: {}", status, body_text).into())
    }
}

fn prompt(msg: &str) -> String {
    print!("{}", msg);
    io::stdout().flush().unwrap();
    let mut input = String::new();
    io::stdin().read_line(&mut input).unwrap();
    input.trim().to_string()
}

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    println!("FCM Notification from Rust");

    let args: Vec<String> = env::args().collect();

    let mut key_path = DEFAULT_SERVICE_ACCOUNT_PATH.to_string();
    let mut target_token: Option<String> = None;
    let mut cli_movie_id: Option<String> = None;
    let mut cli_title: Option<String> = None;
    let mut cli_body: Option<String> = None;

    let mut i = 1;
    while i < args.len() {
        match args[i].as_str() {
            "--key" if i + 1 < args.len() => {
                key_path = args[i + 1].clone();
                i += 2;
            }
            "--token" if i + 1 < args.len() => {
                target_token = Some(args[i + 1].clone());
                i += 2;
            }
            "--id" | "--movie-id" if i + 1 < args.len() => {
                cli_movie_id = Some(args[i + 1].clone());
                i += 2;
            }
            "--title" if i + 1 < args.len() => {
                cli_title = Some(args[i + 1].clone());
                i += 2;
            }
            "--body" if i + 1 < args.len() => {
                cli_body = Some(args[i + 1].clone());
                i += 2;
            }
            _ => {
                i += 1;
            }
        }
    }

    if !Path::new(&key_path).exists() {
        println!("\nService account file not found at: {}", key_path);
        key_path = prompt("Enter path to service account JSON: ");
        if !Path::new(&key_path).exists() {
            eprintln!("Error: File not found at {}", key_path);
            return Ok(());
        }
    }

    let sa_content = fs::read_to_string(&key_path)
        .map_err(|e| format!("Failed to read service account key file: {}", e))?;
    let sa_key: ServiceAccountKey = serde_json::from_str(&sa_content)
        .map_err(|e| format!("Invalid service account JSON format: {}", e))?;

    println!("[OK] Service Account: {}", sa_key.client_email);
    println!("[OK] Project: {}", sa_key.project_id);

    let http_client = reqwest::Client::new();

    print!("Connecting to Firebase OAuth2... ");
    io::stdout().flush().unwrap();
    let access_token = get_access_token(&http_client, &sa_key).await?;
    println!("Connected successfully!");

    // Non-interactive CLI mode
    if let (Some(token), Some(movie_id)) = (target_token.clone(), cli_movie_id) {
        let title = cli_title.unwrap_or_else(|| "🎬 Recommended Movie".to_string());
        let body = cli_body.unwrap_or_else(|| format!("Tap to open movie details (ID: {})", movie_id));

        println!("\nSending message to device:");
        println!("  Movie ID: {}", movie_id);
        println!("  Title:    {}", title);
        println!("  Body:     {}", body);
        println!("  Token:    {}", token);

        match send_fcm_movie_message(
            &http_client,
            &access_token,
            &sa_key.project_id,
            &token,
            &movie_id,
            &title,
            &body,
        )
        .await
        {
            Ok(resp) => println!("\n[SUCCESS] Message delivered to FCM! Response:\n{}", resp),
            Err(e) => eprintln!("\n[ERROR] Delivery failed: {}", e),
        }
        return Ok(());
    }

    // Interactive Menu Mode
    println!("\n--- Interactive Demo Mode ---");
    let device_token = match target_token {
        Some(t) => t,
        None => {
            println!("You can copy your device FCM token from Android Studio Logcat (filter: 'FCM_TOKEN').");
            prompt("Paste your FCM Device Token: ")
        }
    };

    if device_token.is_empty() {
        eprintln!("Error: Token cannot be empty.");
        return Ok(());
    }

    loop {
        println!("\nSelect a movie demo to send to your app:");
        println!("1. Send Inception (ID: 27205)");
        println!("2. Send Interstellar (ID: 157336)");
        println!("3. Send The Dark Knight (ID: 155)");
        println!("4. Send Custom Movie (Enter ID & Title)");
        println!("5. Exit");

        let choice = prompt("Enter choice (1-5): ");
        let (movie_id, title, body) = match choice.as_str() {
            "1" => (
                "27205".to_string(),
                "🎬 Inception Recommended!".to_string(),
                "Tap here to explore Inception details.".to_string(),
            ),
            "2" => (
                "157336".to_string(),
                "🚀 Interstellar Recommended!".to_string(),
                "Tap here to explore Interstellar details.".to_string(),
            ),
            "3" => (
                "155".to_string(),
                "🦇 The Dark Knight Recommended!".to_string(),
                "Tap here to explore The Dark Knight details.".to_string(),
            ),
            "4" => {
                let id = prompt("Enter Movie ID (numbers): ");
                if id.is_empty() {
                    println!("Invalid ID.");
                    continue;
                }
                let movie_name = prompt("Enter Movie Title: ");
                let t = if movie_name.is_empty() {
                    "🎬 Movie Recommendation".to_string()
                } else {
                    format!("🎬 Watch {}", movie_name)
                };
                let b = format!("Tap here to open details for {}", if movie_name.is_empty() { "this movie" } else { &movie_name });
                (id, t, b)
            }
            "5" | "exit" | "q" => {
                println!("Exiting demo.");
                break;
            }
            _ => {
                println!("Invalid choice. Please select 1-5.");
                continue;
            }
        };

        print!("\nSending message to device (Movie ID: {})... ", movie_id);
        io::stdout().flush().unwrap();
        match send_fcm_movie_message(
            &http_client,
            &access_token,
            &sa_key.project_id,
            &device_token,
            &movie_id,
            &title,
            &body,
        )
        .await
        {
            Ok(resp) => {
                println!("Delivered!\nFCM Server Response: {}", resp);
                println!(">> Now tap the notification on your phone/emulator to see the movie screen open! <<\n");
            }
            Err(e) => eprintln!("Error: {}\n", e),
        }
    }

    Ok(())
}
