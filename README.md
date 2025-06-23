# Winget Monitor

A lightweight utility to monitor available Windows program updates using the Windows Package Manager (winget).

## Description

Winget Monitor is a Kotlin application that checks for available updates to installed software on Windows systems using the `winget` command-line tool. It parses the output, creates a structured JSON file with update information, and logs the results.

## Features

- Automatically detects available updates for installed software
- Generates a structured JSON output file with update details
- Logs all activities for troubleshooting
- Handles cases where winget is not installed or no updates are available
- Minimal resource usage

## Requirements

- Windows operating system
- [Windows Package Manager (winget)](https://github.com/microsoft/winget-cli) installed
- Java 21 or higher

## Installation

1. Ensure you have Java 21 or higher installed
2. Clone this repository:
   ```
   git clone https://github.com/yourusername/winget-monitor.git
   ```
3. Build the application:
   ```
   cd winget-monitor
   ./gradlew build
   ```

## Usage

Run the application using Gradle:

```
./gradlew run
```

Or build a JAR file and run it directly:

```
./gradlew jar
java -jar build/libs/winget-monitor-1.0-SNAPSHOT.jar
```

### Output

The application creates two files in your home directory:

1. `.winget-monitor` - JSON file containing update information
2. `.winget-monitor.log` - Log file with execution details

Example JSON output:

```json
{
  "timestamp": "2023-05-15T10:30:45.123Z",
  "updateCount": 2,
  "updates": [
    {
      "name": "Mozilla Firefox",
      "version": "112.0.1",
      "source": "winget"
    },
    {
      "name": "Visual Studio Code",
      "version": "1.77.3",
      "source": "winget"
    }
  ]
}
```

## Integration Ideas

- Schedule the application to run periodically using Windows Task Scheduler
- Create a dashboard to visualize available updates
- Set up notifications when important updates are available

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Disclaimer

This project was mostly created with AI-assisted coding using Junie, Jetbrains AI and ChatGPT. The AI tools were used to help with code generation, problem-solving, commit messages, and documentation.
