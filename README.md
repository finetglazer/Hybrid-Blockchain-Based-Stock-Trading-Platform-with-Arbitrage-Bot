# StockTrader Platform

A microservices-based stock trading platform that allows users to manage accounts, trade stocks, and track portfolios.

## 📝 Project Overview

StockTrader is an educational microservices architecture project that simulates a real-world stock trading platform. The system enables users to create accounts, deposit funds, execute trades, and manage their investment portfolios through a clean, intuitive interface.

## Key Features

- **User Authentication & Security**: Secure user authentication with JWT and two-factor authentication
- **Account Management**: Create and manage trading accounts with deposits and withdrawals
- **Order Processing**: Place market and limit orders with real-time execution
- **Portfolio Tracking**: Monitor positions, gains/losses, and overall portfolio performance
- **Mock Brokerage Integration**: Simulated stock exchange with realistic market behavior
- **Distributed Transaction Management**: Saga pattern implementation for data consistency

## 🏗️ Architecture

The project follows a microservices architecture with the following components:

### Core Microservices

#### API Gateway
- Routes client requests to appropriate services
- Handles authentication and authorization
- Manages cross-cutting concerns like CORS and rate limiting

#### UserService
- User authentication and profile management
- JWT token generation and validation
- Trading permission management
- Two-factor authentication (2FA)

#### AccountService
- Trading account management
- Fund deposits and withdrawals
- Balance tracking and fund reservations
- Payment method management

#### OrderService
- Order creation and management
- Order validation
- Order lifecycle tracking

#### PortfolioService
- Position tracking
- Portfolio analysis
- Watchlist management

#### MarketDataService
- Stock information management
- Price updates
- Historical data access

#### MockBrokerageService
- Simulates a real stock exchange
- Provides realistic order execution
- Generates mock market data

#### SagaOrchestratorService
- Coordinates distributed transactions
- Manages compensating actions for failures
- Ensures data consistency across services

### Communication Patterns

- **Synchronous Communication**: REST APIs for direct service-to-service communication
- **Asynchronous Communication**: Kafka-based event-driven architecture for saga orchestration
- **Saga Pattern**: Orchestration-based sagas for distributed transactions

## 💻 Technical Stack

### Backend
- **Java Spring Boot**: Main application framework
- **MongoDB**: Document database for most services
- **Redis**: For caching and distributed state
- **Apache Kafka**: Message broker for event-driven communication
- **Spring Security**: For authorization and security controls

### Frontend
- **React**: Modern UI framework
- **React Router**: For navigation
- **Axios**: For API requests
- **TailwindCSS**: For styling

### Deployment
- **Docker**: Container technology
- **Railway**: Deployment platform

## 🚀 Getting Started

### Prerequisites
- Java 17+
- Docker and Docker Compose
- MongoDB
- Redis
- Node.js and npm (for frontend)

### Installation

1. Clone the repository
```bash
git clone https://github.com/yourusername/stocktrader-platform.git
cd stocktrader-platform
```

2. Set up environment variables
```bash
cp .env.example .env
# Edit .env file with your configurations
```

3. Start the infrastructure services
```bash
docker-compose up -d
```

4. Start the backend services
```bash
cd BE
mvn clean package
```

5. Start the frontend
```bash
cd FE/investwebsite
npm install
npm run dev
```

6. Access the application
```
API Gateway: http://localhost:8080
Frontend: http://localhost:5173
Swagger Documentation: http://localhost:8080/swagger-ui.html
```

## 📋 Development Roadmap

The project follows an agile development approach with 2-week sprints:

- Sprint 1: User Authentication Service ✅
- Sprint 2: Core Account Management (in progress)
- Sprint 3: Order Processing & Execution
- Sprint 4: Portfolio Management
- Sprint 5: Market Data Integration
- Sprint 6+: Advanced Features & Refinement

## 📐 System Flows

### Order Creation Flow
1. User places order through API Gateway
2. OrderService creates a pending order
3. SagaOrchestrator orchestrates the order validation and execution process
4. MockBrokerageService simulates market execution
5. PortfolioService updates user positions
6. Order is completed and user is notified

### Fund Deposit Flow
1. User initiates deposit through API Gateway
2. AccountService processes the deposit request
3. SagaOrchestrator coordinates the transaction
4. Mock payment processor handles the payment
5. Account balance is updated
6. User is notified of successful deposit

## 🔐 Security Features

- JWT-based authentication
- Role-based access control
- Two-factor authentication via Firebase
- Enhanced security for financial operations
- Masked account numbers and sensitive data

## 📚 Documentation

- API Documentation: /swagger-ui.html
- Architecture Document: docs/architecture.md
- Development Guidelines: docs/development.md

## 🛠️ Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 📞 Contact

Team05 - ...

⭐️ From team 05