# PauseLinks

**Put your website, store, Discord and rules one click away — right in the player's pause menu.**

PauseLinks is a small, free Paper plugin that uses Minecraft's built-in *Server Links* and *Dialog* features
(1.21.6+). No resource pack, no client mod, nothing for players to install.

Made and used in production by [**Craftavia**](https://craftavia.net), a Turkish Minecraft network
(`play.craftavia.net`).

## Features

- **Server Links list** – fills Minecraft's own *Escape → Server Links...* screen with your links.
- **Your own pause menu button** – adds a custom button (with your text and colors) that opens a
  link screen. Buttons can open a URL **or run a command** (for example `/spawn`).
- **Clickable chat list** – `/links` shows every link in chat with hover text.
- **Shortcut commands** – `/discord`, `/website` or any command you define, each showing one link.
- **Per-player language** – players see messages in their client language. English and Turkish are
  bundled; add any language by dropping a `messages_<code>.properties` file in the plugin folder.
- **MiniMessage everywhere** – colors, gradients and formatting in names, titles and messages.
- **Live reload** – `/links reload` re-sends the list to online players, no reconnect needed.
- Lightweight: no dependencies, no database, no network calls, no metrics.

## Requirements

| | |
|---|---|
| Server | Paper (or a fork such as Purpur) **1.21.6 – 1.21.11, 26.x** |
| Java | 21 or newer |
| Client | Java Edition 1.21.6+ (Bedrock players via Geyser simply do not see the pause menu parts) |

## Installation

1. Download `PauseLinks-x.y.z.jar` from [Releases](https://github.com/craftavia/PauseLinks/releases).
2. Put it in your `plugins/` folder and start the server once.
3. Edit `plugins/PauseLinks/config.yml` (replace the `example.com` links with yours).
4. Run `/links reload`. If you changed the `pause-button` section, restart the server once
   (the button is delivered through a data pack, which Minecraft only loads at startup).

## Commands and permissions

| Command | Description | Permission | Default |
|---|---|---|---|
| `/links` (`/serverlinks`) | Shows all links in chat | `pauselinks.use` | everyone |
| `/discord`, `/website`, … | Shows one link (configurable in `shortcut-commands`) | `pauselinks.use` | everyone |
| `/links reload` | Reloads config and messages | `pauselinks.admin` | op |
| `/links status` | Shows link count and version | `pauselinks.admin` | op |

## Configuration

```yaml
default-language: en
chat-list: true          # /links in chat
server-links: true       # Minecraft's "Server Links..." screen

pause-button:
  enabled: true
  button: "<aqua>Server Links...</aqua>"
  title: "<aqua><bold>MY SERVER</bold></aqua>"
  description:
    - "<gray>Visit our website, store and Discord with the buttons below.</gray>"
  columns: 2
  button-width: 150
  show-address: true
  close: "<red>Close</red>"

shortcut-commands:
  discord: { link: discord, aliases: [dc] }

links:
  website:
    name: "<aqua>▸ Website</aqua>"
    description: "Our home page"
    url: "https://example.com"
  spawn:
    name: "<yellow>▸ Spawn</yellow>"
    description: "Teleport to spawn"
    command: "/spawn"     # runs a command instead of opening a URL
    chat: false           # hide from /links
```

The full file with comments is created on first start.

### Notes

- Minecraft's own *Server Links...* button text comes from the client and cannot be changed;
  that is why PauseLinks adds its own button next to it.
- If another plugin or data pack also adds a pause screen dialog, Minecraft groups them under a
  *Custom Options...* button. This is vanilla behavior.
- Commands run from a dialog button go through your normal command permissions and command
  whitelist plugins.

## Building

```bash
./gradlew build
```

The jar is written to `build/libs/`.

## Türkçe

PauseLinks, oyuncunun **Escape (duraklatma) menüsüne** sitenizi, mağazanızı, Discord'unuzu ve
kurallarınızı ekleyen ücretsiz bir Paper eklentisidir. Minecraft'ın kendi *Sunucu Bağlantıları* ve
*Dialog* özelliklerini kullanır, oyuncunun hiçbir şey kurması gerekmez.

- Escape menüsündeki *Sunucu Bağlantıları* listesini doldurur.
- Menüye kendi renkli butonunuzu ekler. Butonlar adres açabilir ya da komut çalıştırabilir (`/spawn`).
- `/links` ile sohbette tıklanabilir liste gösterir. `/discord` gibi kısa komutlar tanımlanabilir.
- Oyun dili Türkçe olan oyuncu mesajları Türkçe görür (`messages_tr.properties`).

Kurulum: jar'ı `plugins/` klasörüne atın, sunucuyu bir kez açın, `config.yml` içindeki
`example.com` adreslerini kendi adreslerinizle değiştirin ve `/links reload` yazın.
`pause-button` bölümünü değiştirdiyseniz sunucuyu bir kez yeniden başlatın.

Craftavia ekibi tarafından geliştirildi ve [craftavia.net](https://craftavia.net) sunucularında
kullanılıyor.

## License

[MIT](LICENSE) © Craftavia
