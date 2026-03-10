// swift-tools-version: 6.0

import PackageDescription

let package = Package(
    name: "JSBridge",
    platforms: [.iOS(.v16)],
    products: [
        .library(name: "JSBridge", targets: ["JSBridge"]),
    ],
    dependencies: [
        .package(url: "https://github.com/kibotu/Orchard", from: "1.0.9"),
    ],
    targets: [
        .target(
            name: "JSBridge",
            dependencies: [
                .product(name: "Orchard", package: "orchard"),
            ],
            resources: [
                .copy("Resources/bridge.js"),
            ]
        ),
    ]
)
