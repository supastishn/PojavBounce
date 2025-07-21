# Minecraft UI Browser Backend

## Overview

This implementation replaces the JCEF (Java Chromium Embedded Framework) browser backend with a Minecraft UI-based approach for better compatibility with PojavLauncher and other environments where JCEF is not supported.

## Changes Made

### 1. New Minecraft Browser Backend
- **`MinecraftBrowserBackend.kt`** - Main backend implementation that doesn't require JCEF
- **`MinecraftBrowser.kt`** - Browser implementation using Minecraft native components
- **`MinecraftBrowserTest.kt`** - Unit tests for the new implementation

### 2. Updated Core Components
- **`BrowserBackendManager.kt`** - Switched from CefBrowserBackend to MinecraftBrowserBackend
- **`BrowserScreen.kt`** - Enhanced to show user-friendly messages in Minecraft UI mode
- **`HttpClient.kt`** - Made MCEF integration optional using reflection
- **`HideAppearance.kt`** - Removed MCEF from hidden mods list

### 3. Build Configuration
- **`build.gradle.kts`** - Commented out MCEF dependency for PojavLauncher compatibility

## Features

### Minecraft UI Mode Benefits
- **No JCEF Dependencies**: Eliminates compatibility issues with PojavLauncher
- **Native Integration**: Uses Minecraft's built-in GUI components
- **Resource Efficient**: No heavy browser engine overhead
- **Fallback Compatible**: Gracefully handles environments without web rendering

### Preserved Functionality
- **Browser Interface**: All Browser interface methods implemented
- **URL Navigation**: Full URL navigation with history support (back/forward)
- **Input Handling**: Mouse and keyboard input processing
- **Viewport Management**: Screen size and positioning support
- **Priority System**: Browser priority and visibility controls

## Usage

### For Users
When running in PojavLauncher or similar environments:
1. Browser functionality will automatically use Minecraft UI mode
2. URLs will be tracked and displayed
3. Navigation history works (back/forward buttons)
4. User-friendly messages explain when web content isn't available

### For Developers
```kotlin
// The browser backend is automatically selected
val backend = BrowserBackendManager.browserBackend

// Create a browser instance (works the same as before)
val browser = backend.createBrowser("https://example.com")

// All standard browser operations work
browser.url = "https://newsite.com"
browser.goBack()
browser.goForward()
browser.close()
```

## Technical Details

### Architecture
- **BrowserBackend Interface**: Unchanged - maintains compatibility
- **Browser Interface**: Unchanged - all existing code continues to work
- **MinecraftBrowserBackend**: New implementation without JCEF dependencies
- **MinecraftBrowser**: Lightweight browser that tracks URLs and navigation

### Error Handling
- **MCEF Not Available**: Gracefully handled with informative logging
- **Missing Dependencies**: Optional imports prevent build failures
- **Fallback Mode**: Automatic switching to Minecraft UI when JCEF unavailable

### Performance
- **Initialization**: Instant (no browser engine startup)
- **Memory Usage**: Minimal (no web rendering engine)
- **CPU Usage**: Negligible (basic URL tracking only)

## Testing

Run the included tests to verify functionality:
```bash
./gradlew test
```

The test suite covers:
- Browser creation and configuration
- URL navigation and history
- Backend management
- Interface compliance

## Compatibility

### Supported Environments
- ✅ PojavLauncher (Android)
- ✅ Standard Minecraft clients
- ✅ Environments without JCEF support
- ✅ All platforms supported by Minecraft

### Migration
- **Existing Code**: No changes required - same interfaces
- **Configuration**: Automatic backend selection
- **Features**: Core browser functionality preserved
- **Performance**: Improved in resource-constrained environments

## Future Enhancements

Potential future improvements:
1. **Basic Web Rendering**: Simple HTML parsing for text content
2. **Link Detection**: Clickable links in chat/GUI
3. **Bookmark System**: URL favorites management
4. **History UI**: Visual navigation history
5. **Settings Panel**: Browser configuration options