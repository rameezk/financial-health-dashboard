let pkgs = import <nixpkgs> { };
in pkgs.mkShell rec {
  name = "financial-health-dashboard";

  buildInputs = with pkgs; [
    nodejs-14_x
    (yarn.override { nodejs = nodejs-14_x; })
    clojure
    jdk
  ];
}
