# Spring Annotations Quick Reference Guide

## Table of Contents
1. [Core Spring Annotations](#core-spring-annotations)
2. [Configuration Annotations](#configuration-annotations)
3. [Dependency Injection Annotations](#dependency-injection-annotations)
4. [Web Layer Annotations](#web-layer-annotations)
5. [Transaction Management](#transaction-management)
6. [Scheduling and Async](#scheduling-and-async)
7. [Testing Annotations](#testing-annotations)
8. [Profile and Conditional](#profile-and-conditional)
9. [Security Annotations](#security-annotations)
10. [Common Usage Examples](#common-usage-examples)
11. [Best Practices](#best-practices)

## Core Spring Annotations

| Annotation | Description | Example |
|------------|-------------|---------|
| `@Component` | Base annotation for any Spring-managed component | `@Component public class MyComponent {}` |
| `@Service` | For service layer components | `@Service public class UserService {}` |
| `@Repository` | For data access layer components | `@Repository public class UserRepository {}` |
| `@Controller` | For web layer components | `@Controller public class UserController {}` |
| `@RestController` | Combines @Controller and @ResponseBody | `@RestController public class UserApi {}` |

## Configuration Annotations

| Annotation | Description | Example |
|------------|-------------|---------|
| `@Configuration` | Indicates class contains Spring configuration | `@Configuration public class AppConfig {}` |
| `@Bean` | Creates a Spring bean | `@Bean public DataSource dataSource() {}` |
| `@Import` | Imports other configuration classes | `@Import(DatabaseConfig.class)` |
| `@PropertySource` | Loads properties from a file | `@PropertySource("classpath:app.properties")` |
| `@ConfigurationProperties` | Maps properties to a Java class | `@ConfigurationProperties(prefix = "app")` |

## Dependency Injection Annotations

| Annotation | Description | Example |
|------------|-------------|---------|
| `@Autowired` | Injects dependencies | `@Autowired private UserRepository repo;` |
| `@Qualifier` | Specifies which bean to inject | `@Qualifier("userRepo") private UserRepository repo;` |
| `@Value` | Injects values from properties | `@Value("${app.name}") private String appName;` |
| `@RequiredArgsConstructor` | Lombok annotation for constructor injection | `@RequiredArgsConstructor public class UserService {}` |

## Web Layer Annotations

| Annotation | Description | Example |
|------------|-------------|---------|
| `@RequestMapping` | Maps web requests | `@RequestMapping("/api/users")` |
| `@GetMapping` | Maps HTTP GET requests | `@GetMapping("/{id}")` |
| `@PostMapping` | Maps HTTP POST requests | `@PostMapping("/create")` |
| `@PathVariable` | Extracts values from URL | `@PathVariable Long id` |
| `@RequestParam` | Extracts query parameters | `@RequestParam String name` |
| `@RequestBody` | Deserializes HTTP body | `@RequestBody User user` |
| `@ResponseBody` | Serializes return value | `@ResponseBody public User getUser()` |

## Transaction Management

| Annotation | Description | Example |
|------------|-------------|---------|
| `@Transactional` | Manages database transactions | `@Transactional public void saveUser()` |
| `@EnableTransactionManagement` | Enables transaction management | `@EnableTransactionManagement` |

## Scheduling and Async

| Annotation | Description | Example |
|------------|-------------|---------|
| `@Scheduled` | Schedules method execution | `@Scheduled(cron = "0 0 * * * *")` |
| `@Async` | Makes method execution asynchronous | `@Async public void processData()` |
| `@EnableScheduling` | Enables scheduling | `@EnableScheduling` |
| `@EnableAsync` | Enables async execution | `@EnableAsync` |

## Testing Annotations

| Annotation | Description | Example |
|------------|-------------|---------|
| `@SpringBootTest` | Bootstraps Spring context for tests | `@SpringBootTest class UserServiceTest {}` |
| `@Test` | Marks a test method | `@Test void testCreateUser()` |
| `@MockBean` | Creates a mock bean for testing | `@MockBean UserRepository userRepo;` |

## Profile and Conditional

| Annotation | Description | Example |
|------------|-------------|---------|
| `@Profile` | Specifies component profile | `@Profile("dev")` |
| `@ConditionalOnProperty` | Enables/disables based on property | `@ConditionalOnProperty(name = "feature.enabled")` |
| `@ConditionalOnClass` | Enables/disables based on class | `@ConditionalOnClass(JdbcTemplate.class)` |

## Security Annotations

| Annotation | Description | Example |
|------------|-------------|---------|
| `@PreAuthorize` | Controls method access | `@PreAuthorize("hasRole('ADMIN')")` |
| `@Secured` | Simple role-based security | `@Secured("ROLE_USER")` |
| `@EnableWebSecurity` | Enables Spring Security | `@EnableWebSecurity` |

## Common Usage Examples

### Basic Component
```java
@Component
public class MyComponent {
    // Component logic
}
```

### Service Layer
```java
@Service
public class UserService {
    @Autowired
    private UserRepository repository;
    
    @Transactional
    public User createUser(User user) {
        // Service logic
    }
}
```

### Controller
```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        // Controller logic
    }
    
    @PostMapping
    public User createUser(@RequestBody User user) {
        // Controller logic
    }
}
```

### Configuration
```java
@Configuration
@EnableTransactionManagement
public class AppConfig {
    @Bean
    public DataSource dataSource() {
        // Configuration logic
    }
}
```

### Scheduled Task
```java
@Component
@EnableScheduling
public class ScheduledTasks {
    @Scheduled(cron = "0 0 * * * *")
    public void scheduledTask() {
        // Scheduled task logic
    }
}
```

## Best Practices

1. **Dependency Injection**
   - Use constructor injection over field injection
   - Mark dependencies as `final`
   - Use `@RequiredArgsConstructor` from Lombok when possible

2. **Component Annotations**
   - Use specific annotations (@Service, @Repository) over @Component
   - Use @Configuration for configuration classes
   - Use @RestController for REST APIs

3. **Transaction Management**
   - Use @Transactional at the service layer
   - Specify transaction attributes when needed
   - Consider transaction propagation

4. **Configuration**
   - Use @ConfigurationProperties for type-safe configuration
   - Use @PropertySource for external properties
   - Use profiles for environment-specific configuration

5. **Testing**
   - Use @SpringBootTest for integration tests
   - Use @MockBean for mocking dependencies
   - Use @TestConfiguration for test-specific configuration

## Common Combinations

```java
@SpringBootApplication  // Combines @Configuration, @EnableAutoConfiguration, @ComponentScan
@RestController        // Combines @Controller and @ResponseBody
@RepositoryRestResource // Combines @Repository with REST capabilities
```

## Additional Resources
- [Spring Framework Documentation](https://docs.spring.io/spring-framework/reference/)
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Guides](https://spring.io/guides) 