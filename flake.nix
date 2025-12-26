{
  description = "LiquidBounce development environment";

  inputs = { nixpkgs.url = "github:NixOS/nixpkgs/nixos-25.05"; };

  outputs = { self, nixpkgs }:
    let
      system = "x86_64-linux";
      pkgs = nixpkgs.legacyPackages.${system};
      libs = with pkgs; [
        temurin-bin
        pciutils
        nodejs_24
        libpulseaudio
        libGL
        glfw
        openal
        # stdenv.cc.cc.lib
        git
        xorg.libX11
        xorg.libXcursor
        flite

        # CEF (chromium) dependencies
        # libcef

        libgbm
        glib
        nss
        nspr
        atk
        at-spi2-atk
        libdrm
        expat
        xorg.libxcb
        libxkbcommon
        xorg.libX11
        xorg.libXcomposite
        xorg.libXdamage
        xorg.libXext
        xorg.libXfixes
        xorg.libXrandr
        libgbm
        gtk3
        pango
        cairo
        alsa-lib
        dbus
        at-spi2-core
        cups
        xorg.libxshmfence
      ];

    in {
      devShells.${system}.default = pkgs.mkShell {
        packages = libs;
        buildInputs = libs;

        LD_LIBRARY_PATH = pkgs.lib.makeLibraryPath libs;
      };
    };
  nixConfig.bash-prompt-suffix = "[liquidbounce] ";
}
