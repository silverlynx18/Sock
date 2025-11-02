// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "SockApp",
    defaultLocalization: "en",
    platforms: [
        .iOS(.v17)
    ],
    products: [
        .library(
            name: "SockApp",
            targets: ["SockApp"]
        )
    ],
    dependencies: [
        // Room/DataStore equivalents will live behind local persistence protocols.
    ],
    targets: [
        .target(
            name: "SockApp",
            resources: [
                .process("Resources")
            ]
        ),
        .testTarget(
            name: "SockAppTests",
            dependencies: ["SockApp"]
        )
    ]
)
