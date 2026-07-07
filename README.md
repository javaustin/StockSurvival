# StockSurvival

StockSurvival is a basic quality of life plugin for survival servers. It depends on [CXYZ](https://github.com/javaustin/cxyz), my core plugin.

## Features

- **Spawn protection**: Applies temporary no-damage ticks after joining.
- **Newbie protection**: Gives players a playtime-based grace period upon their first join (access with `/prot`).
- **Combat logger**: Players who quit the game while in combat with another player are killed.
- **Xray notifications**: Broadcasts or forwards notifications when configured valuable blocks are mined.
- **End gating**: Can block End portal travel through configuration.
- **Discord/webhook integration**: Sends join, leave, death, advancement, server start, and server stop messages to a configured webhook.

## Setup / Install

### Requirements

- A Paper server compatible with the API version declared in `plugin.yml`
- Java 25
- The [**CXYZ**](https://github.com/javaustin/cxyz) plugin installed on the server

### Build

This project uses Maven.

Run:
```bash
mvn clean package
```

The shaded plugin jar will be created in `target/`.

### Install on a server

1. Build the plugin jar or use the provided jars in `target/`.
2. Copy the generated jar into your server’s `plugins/` directory.
3. Install the **CXYZ** plugin on the same server, since StockSurvival depends on it at runtime.
4. Start or restart your server

### Configuration
[View config.yml](https://github.com/javaustin/StockSurvival/blob/main/src/main/resources/config.yml)  
[View messages.yml](https://github.com/javaustin/StockSurvival/blob/main/src/main/resources/messages.yml)
