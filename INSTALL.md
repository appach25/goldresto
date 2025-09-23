# Gold Resto Management System - Installation Guide

## Prerequisites
1. Java 17 or later
2. PostgreSQL 12 or later
3. Windows 10 or later

## Installation Steps

1. Install PostgreSQL if not already installed:
   - Download from: https://www.postgresql.org/download/windows/
   - Install with default port (5432)
   - Remember your PostgreSQL password

2. Create Database:
   - Open pgAdmin or PostgreSQL command prompt
   - Create a new database named 'goldresto'

3. Run the Installer:
   - Double-click `install.bat`
   - Wait for the installation to complete
   - A desktop shortcut will be created automatically

4. Configure Database Connection:
   - Navigate to `C:\Program Files\GoldResto`
   - Edit `application.properties`
   - Update database username and password

5. Start the Application:
   - Double-click the desktop shortcut "Gold Resto"
   - Access the application at http://localhost:8081
   - Default login: admin/admin

## Printer Setup

1. Install RONGTA printer drivers
2. Connect the receipt printer via USB
3. Set as default printer

## Troubleshooting

- If the application doesn't start, check logs in `C:\Program Files\GoldResto\logs`
- Ensure PostgreSQL service is running
- Verify Java is installed correctly: `java -version`

## Support

For technical support, please contact your system administrator.
