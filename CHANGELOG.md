# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.1.1] - 2026-01-01

### Fixed
- **Netty 4.2 API Migration** - Eliminated deprecation warnings
  - Migrated from `NioEventLoopGroup` to `MultiThreadIoEventLoopGroup`
  - Uses `NioIoHandler.newFactory()` following Netty 4.2.x best practices
  - Compatible with Spring Boot 4.0.1's Netty 4.2.9.Final dependency
  - All tests pass with no functional changes

### Added
- **NETTY_4.2_MIGRATION.md** - Migration guide for users with custom EventLoopGroup beans

## [1.1.0] - 2026-01-01

### Added
- **Interceptor Chain** - Support for cross-cutting concerns (logging, retry, metrics)
  - `ClientInterceptor` interface for implementing custom interceptors
  - `InterceptorChain` for executing interceptors in order
  - `InterceptorContext` for passing request/response data between interceptors
- **Built-in Interceptors**
  - `TransactionInterceptor` - Automatic transaction ID generation and MDC management
  - `LoggingInterceptor` - Request/response logging
  - `RetryInterceptor` - Exponential backoff retry mechanism
  - `MetricsInterceptor` - Micrometer metrics collection (Prometheus, Grafana integration)
- **ExecutorService Integration** - Dedicated thread pool for CompletableFuture callbacks
  - `tcpClientExecutor` bean for handling asynchronous callbacks
  - Prevents CompletableFuture callbacks from blocking Netty EventLoop threads
  - Uses CachedThreadPool for dynamic scaling (60s idle thread timeout)
  - Improves performance and responsiveness under high load
- **Comprehensive Tests**
  - `InterceptorChainTest` - Unit tests for interceptor execution order, response modification, exception propagation
  - `InterceptorE2ETest` - E2E tests with custom interceptors, request counter, and TransactionInterceptor
  - `MockTcpServer` - Simple TCP server implementation for E2E testing
- **Documentation**
  - `AGENT_GUIDE.md` - Project design, coding style, test guide, and workflow documentation
  - `NEXT_SESSION_CONTEXT.md` - v1.0.0 status and next session goals

### Changed
- **TcpMessageClient** - Now accepts ExecutorService for callback execution
- **TcpClientAutoConfiguration** - Added ExecutorService bean with @Primary annotation
- **TcpClientFactoryBean** - Explicitly requests tcpClientExecutor by name
- Enhanced test coverage with interceptor chain tests
- Improved project documentation structure

## [1.0.0] - 2025-12-30

### Removed
- **HTTP Client Support** - Removed all HTTP client-related code and tests
  - Deleted `@EnableHttpClient`, `@HttpClient` annotations
  - Removed `HttpClientAutoConfiguration`, `HttpClientFactoryBean`
  - Removed HTTP client integration tests (E2E)

### Rationale
- Focus exclusively on TCP client functionality
- Spring already provides excellent HTTP client solutions (RestClient, WebClient)
- TCP client addresses a unique gap in the Spring ecosystem for legacy system integration

### Added
- Architecture and migration planning documents
  - `CURRENT_ARCHITECTURE.md` - System architecture and data flow
  - `HTTP_EXCHANGE_MIGRATION.md` - HttpExchange-based HTTP client migration plan
  - `INTERCEPTOR_CHAIN_DESIGN.md` - TCP/HTTP client interceptor chain design

## [0.3.0] - 2025-12-28

### Added
- **Interceptor Chain Implementation**
  - `ClientInterceptor`, `InterceptorChain`, `InterceptorContext` for TCP/HTTP clients
  - Support for cross-cutting concerns (logging, metrics, retry logic)
- **HttpExchange-based HTTP Client Refactoring**
  - GET/POST/PUT/DELETE request support
  - `@PathVariable` and other annotation support
- **E2E Integration Tests**
  - HttpExchange and interceptor chain behavior validation
- **Documentation**
  - v0.3.0 release notes and work summary

## [0.2.0] - 2025-12-27

### Added
- **HTTP Client Implementation**
  - RestClient-based synchronous/asynchronous support
  - Same auto-calculate field support as TCP (MESSAGE_LENGTH, CHECKSUM_CRC16, STX/ETX framing)
  - `@EnableHttpClient` annotation for HTTP client activation
- **Checksum Support**
  - `ChecksumUtil` with CRC16/CRC32/XOR checksum calculation
  - Auto-calculate fields: `MESSAGE_LENGTH`, `CHECKSUM_CRC16`, `CHECKSUM_CRC32`, `CHECKSUM_XOR`
- **Enhanced Tests**
  - `AutoCalculateIntegrationTest` - Auto-calculate field validation
  - `FramingIntegrationTest` - STX/ETX framing scenarios
  - TCP/HTTP E2E integration tests
- **Exception Handling Improvements**
  - CompletionException unwrapping and proper exception propagation
  - Synchronous call failure handling by exception type

### Changed
- Improved TcpMessageSerializer and TcpMessageDeserializer
  - Better auto-calculate field padding handling
  - Enhanced validation logic

## [0.1.0] - 2025-12-25

### Added
- **Initial TCP Client Implementation**
  - `@TcpClient` annotation for declarative TCP client definition
  - `@EnableTcpClient` annotation for activating TCP clients
  - `@TcpMessage` and `@TcpField` annotations for message structure definition
- **Fixed-Length Binary Protocol Support**
  - Automatic byte-level serialization/deserialization
  - Character encoding support (UTF-8, EUC-KR, etc.)
  - Padding and alignment (LEFT/RIGHT)
- **Advanced Features**
  - Auto-calculate fields (MESSAGE_LENGTH, CHECKSUM_CRC16/CRC32/XOR)
  - STX/ETX framing with `@Framing` annotation
  - Nested objects and list support
  - Synchronous/asynchronous mode support (CompletableFuture)
- **Spring Boot Integration**
  - AutoConfiguration for seamless Spring Boot integration
  - Dynamic proxy-based client implementation
  - Netty-based asynchronous TCP communication
- **Documentation**
  - `DESIGN_DECISIONS.md` - Architecture design decisions
  - `DEVELOPMENT_GUIDE.md` - Development guide and package structure
  - `USAGE_EXAMPLES.md` - Detailed usage examples
  - `PROJECT_OVERVIEW.md` - Project overview and API reference
- **Test Suite**
  - Unit tests for serialization/deserialization
  - Integration tests for auto-calculate fields and framing
  - E2E tests with mock TCP server

### Technical Details
- Java 21+ support
- Spring Boot 4.x compatibility
- Netty-based TCP client implementation
- Reflection and dynamic proxy for declarative API

[Unreleased]: https://github.com/yourusername/tcp-client-spring-boot-starter/compare/v1.1.0...HEAD
[1.1.0]: https://github.com/yourusername/tcp-client-spring-boot-starter/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/yourusername/tcp-client-spring-boot-starter/compare/v0.3.0...v1.0.0
[0.3.0]: https://github.com/yourusername/tcp-client-spring-boot-starter/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/yourusername/tcp-client-spring-boot-starter/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/yourusername/tcp-client-spring-boot-starter/releases/tag/v0.1.0
