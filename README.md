# Secure Password Manager

A secure password manager application with a Java Spring Boot backend and React TypeScript frontend.

## Features

- **Master Password Authentication**: Secure login with master password (never stored, only derived key)
- **Password Entry Management**: Add, edit, delete, and organize password entries
- **Search & Filter**: Quickly find entries by site name, username, or notes
- **Password Generator**: Generate cryptographically secure random passwords
- **Password Strength Indicator**: Visual feedback on password strength
- **Clipboard Copy**: One-click copy passwords to clipboard
- **Session Security**: Automatic session timeout for protection

## Tech Stack

### Backend
- Java 17+
- Spring Boot 3.x
- Spring Security
- H2 Database (development)
- AES-256-GCM encryption
- PBKDF2 key derivation

### Frontend
- React 18
- TypeScript
- Modern CSS

## Project Structure

```
PWMgr/
├── server/                 # Spring Boot backend
│   ├── src/main/java/com/pwmgr/
│   │   ├── config/        # Configuration classes
│   │   ├── controller/    # REST controllers
│   │   ├── dto/           # Data transfer objects
│   │   ├── model/         # Entity models
│   │   ├── repository/    # Data repositories
│   │   ├── security/      # Security & encryption
│   │   └── service/       # Business logic
│   └── src/main/resources/
├── client/                 # React frontend
│   ├── src/
│   │   ├── components/    # React components
│   │   ├── hooks/         # Custom hooks
│   │   ├── services/      # API services
│   │   └── types/         # TypeScript types
│   └── public/
└── README.md
```

## Setup Instructions

### Prerequisites

- Java 17 or higher
- Node.js 18 or higher
- npm or yarn

### Backend Setup

1. Navigate to the server directory:
   ```bash
   cd server
   ```

2. Copy the environment example file:
   ```bash
   cp .env.example .env
   ```

3. Configure your `.env` file:
   - Generate a secure encryption key for production
   - Adjust session timeout as needed

4. Build and run the application:
   ```bash
   ./mvnw spring-boot:run
   ```

   Or on Windows:
   ```bash
   mvnw.cmd spring-boot:run
   ```

The backend will start on `http://localhost:8080`

### Frontend Setup

1. Navigate to the client directory:
   ```bash
   cd client
   ```

2. Copy the environment example file:
   ```bash
   cp .env.example .env
   ```

3. Install dependencies:
   ```bash
   npm install
   ```

4. Start the development server:
   ```bash
   npm start
   ```

The frontend will start on `http://localhost:3000`

## Security Considerations

### Master Password

- The master password is **never stored** in the database
- PBKDF2 with 600,000 iterations is used for key derivation
- A verification hash is stored to validate the master password

### Encryption

- All sensitive data is encrypted using AES-256-GCM
- Each entry uses a unique IV (Initialization Vector)
- Encryption keys are derived from the master password

### Session Security

- Sessions automatically timeout after configurable period
- Rate limiting prevents brute-force attacks
- Failed login attempts trigger progressive lockouts

### Production Recommendations

1. **Always use HTTPS** in production
2. **Generate a strong encryption key** - never use defaults
3. **Use a production database** (PostgreSQL recommended)
4. **Configure proper CORS** settings
5. **Enable security headers** (CSP, HSTS, etc.)
6. **Regular security audits** and dependency updates
7. **Backup encryption keys** securely (lost key = lost data)

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/setup` | Initialize master password |
| POST | `/api/auth/login` | Authenticate with master password |
| POST | `/api/auth/logout` | End session |
| GET | `/api/entries` | List all password entries |
| POST | `/api/entries` | Create new entry |
| PUT | `/api/entries/{id}` | Update entry |
| DELETE | `/api/entries/{id}` | Delete entry |
| GET | `/api/entries/search` | Search entries |

## Development

### Running Tests

Backend:
```bash
cd server
./mvnw test
```

Frontend:
```bash
cd client
npm test
```

## License

MIT License - See LICENSE file for details.

## Disclaimer

This password manager is provided for educational purposes. While it implements industry-standard security practices, no software is 100% secure. Use at your own risk and always maintain secure backups of critical passwords.
