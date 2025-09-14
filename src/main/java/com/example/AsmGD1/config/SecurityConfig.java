package com.example.AsmGD1.config;

import com.example.AsmGD1.entity.NguoiDung;
import com.example.AsmGD1.service.NguoiDung.CustomOAuth2UserService;
import com.example.AsmGD1.service.NguoiDung.CustomUserDetailsService;
import com.example.AsmGD1.service.NguoiDung.NguoiDungService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.io.PrintWriter;

@Configuration
public class SecurityConfig implements ApplicationContextAware {

    private final CustomUserDetailsService customUserDetailsService;
    private final HttpSession session;
    private final CustomOAuth2SuccessHandler oauth2SuccessHandler;
    private ApplicationContext applicationContext;

    public SecurityConfig(CustomUserDetailsService customUserDetailsService,
                          HttpSession session,
                          CustomOAuth2SuccessHandler oauth2SuccessHandler) {
        this.customUserDetailsService = customUserDetailsService;
        this.session = session;
        this.oauth2SuccessHandler = oauth2SuccessHandler;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandlerEmployees() {
        AccessDeniedHandlerImpl handler = new AccessDeniedHandlerImpl();
        handler.setErrorPage("/polyshoe/login?error=accessDenied");
        return handler;
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandlerCustomers() {
        AccessDeniedHandlerImpl handler = new AccessDeniedHandlerImpl();
        handler.setErrorPage("/customers/login?error=accessDenied");
        return handler;
    }

    @Bean
    public AuthenticationEntryPoint employeeAuthEntryPoint() {
        return (request, response, authException) -> {
            System.out.println("Đang chuyển hướng đến /polyshoe/login do: " + authException.getMessage());
            response.setStatus(HttpServletResponse.SC_FOUND);
            response.sendRedirect("/polyshoe/login");
        };
    }

    @Bean
    public AuthenticationEntryPoint customerAuthEntryPoint() {
        return (request, response, authException) -> {
            System.out.println("Đang chuyển hướng đến /customers/login do: " + authException.getMessage());
            response.setStatus(HttpServletResponse.SC_FOUND);
            response.sendRedirect("/customers/login");
        };
    }

    @Bean
    public AuthenticationEntryPoint defaultAuthEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_FOUND);
            response.sendRedirect("/customers/login");
        };
    }

    @Bean
    public AuthenticationEntryPoint jsonAuthEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            PrintWriter writer = response.getWriter();
            writer.write("{\"success\": false, \"message\": \"Không được phép\"}");
            writer.flush();
        };
    }

    @Bean
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/search", "/api/product/**", "/api/cart/**", "/api/product/*/ratings", "/api/getChiTietSanPham", "/api/cart/check-auth", "/api/cart/get-user", "/api/san-pham/with-chi-tiet", "/api/san-pham/ban-chay", "/api/san-pham/moi-nhat", "/api/product/*/ratings", "/api/getChiTietSanPham").permitAll()
                        .requestMatchers("/api/orders/**").authenticated()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jsonAuthEntryPoint())
                );
        return http.build();
    }
    @Bean
    public SecurityFilterChain employeeSecurityFilterChain(HttpSecurity http,
                                                           CustomerAccessBlockFilter blockFilter) throws Exception {
        NguoiDungService nguoiDungService = applicationContext.getBean(NguoiDungService.class);

        http
                .securityMatcher("/polyshoe/**")
                // VẪN chặn khách hàng lạc vào khu vực nhân sự
                .addFilterBefore(blockFilter, UsernamePasswordAuthenticationFilter.class)
                // ĐÃ BỎ: .addFilterBefore(faceVerificationFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        // Không còn cần permitAll cho các trang face, nhưng giữ cũng không sao.
                        .requestMatchers("/polyshoe/login").permitAll()
                        .requestMatchers("/polyshoe/vi/**").hasRole("ADMIN")
                        .requestMatchers("/polyshoe/verify-success").authenticated()
                        .requestMatchers("/polyshoe/san-pham", "/polyshoe/san-pham/get/**").hasAnyRole("ADMIN", "EMPLOYEE")
                        .requestMatchers("/polyshoe/san-pham/save", "/polyshoe/san-pham/update-status", "/polyshoe/san-pham/upload-image").hasRole("ADMIN")
                        .requestMatchers("/polyshoe/xuat-xu/**", "/polyshoe/mau-sac/**", "/polyshoe/kich-co/**",
                                "/polyshoe/chat-lieu/**", "/polyshoe/kieu-dang/**", "/polyshoe/co-ao/**",
                                "/polyshoe/tay-ao/**", "/polyshoe/danh-muc/**", "/polyshoe/thuong-hieu/**").hasRole("ADMIN")
                        .requestMatchers("/polyshoe/employees/**").hasRole("ADMIN")
                        .requestMatchers("/polyshoe/thong-ke").hasRole("ADMIN")
                        .requestMatchers("/polyshoe/employee-dashboard").hasRole("EMPLOYEE")
                        .requestMatchers("/polyshoe/thong-ke").hasAnyRole("ADMIN", "EMPLOYEE")
                        .requestMatchers("/polyshoe/ban-hang/**", "/polyshoe/hoa-don/**").hasAnyRole("ADMIN", "EMPLOYEE")
                        .requestMatchers("/polyshoe/phieu-giam-gia/**", "/polyshoe/chien-dich-giam-gia/**").hasAnyRole("ADMIN", "EMPLOYEE")
                        .requestMatchers("/polyshoe/customers/**").hasAnyRole("ADMIN", "EMPLOYEE")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/polyshoe/login")
                        .loginProcessingUrl("/polyshoe/login")
                        .successHandler((request, response, authentication) -> {
                            String tenDangNhap = authentication.getName();
                            NguoiDung nguoiDung = nguoiDungService.findByTenDangNhap(tenDangNhap);

                            if (nguoiDung != null) {
                                String vaiTro = nguoiDung.getVaiTro();
                                if ("EMPLOYEE".equalsIgnoreCase(vaiTro)) {
                                    response.sendRedirect("/polyshoe/employee-dashboard");
                                } else if ("ADMIN".equalsIgnoreCase(vaiTro)) {
                                    // ĐÃ BỎ yêu cầu đăng ký/xác thực khuôn mặt, chuyển thẳng vào trang admin
                                    response.sendRedirect("/polyshoe/thong-ke");
                                } else {
                                    response.sendRedirect("/polyshoe/login?error=unauthorizedRole");
                                }
                            } else {
                                response.sendRedirect("/polyshoe/login?error=notfound");
                            }
                        })
                        .failureUrl("/polyshoe/login?error=invalidCredentials")
                        .usernameParameter("tenDangNhap")
                        .passwordParameter("matKhau")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/polyshoe/logout")
                        .logoutSuccessUrl("/polyshoe/login?logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .exceptionHandling(exception -> exception
                        // ĐÃ BỎ entryPoint riêng cho /polyshoe/verify-success (nếu muốn)
                        .defaultAuthenticationEntryPointFor(
                                employeeAuthEntryPoint(),
                                new AntPathRequestMatcher("/polyshoe/**")
                        )
                        .accessDeniedHandler(accessDeniedHandlerEmployees())
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
                        .sessionConcurrency(concurrency -> concurrency
                                .maximumSessions(1)
                                .expiredUrl("/polyshoe/login?expired")
                        )
                        .invalidSessionUrl("/polyshoe/login?invalid")
                )
                .csrf(csrf -> csrf.disable());

        return http.build();
    }


    @Bean
    public SecurityFilterChain customerSecurityFilterChain(HttpSecurity http) throws Exception {
        NguoiDungService nguoiDungService = applicationContext.getBean(NguoiDungService.class);
        http
                .securityMatcher("/customers/**")
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/customers/login",
                                "/customers/register",
                                "/customers/register/**",
                                "/customers/oauth2/register",
                                "/customers/auth/forgot-password",
                                "/customers/auth/verify-otp",
                                "/customers/auth/reset-password",
                                "/customers/auth/resend-otp"
                        ).permitAll()
                        .anyRequest().hasRole("CUSTOMER")
                )
                .formLogin(form -> form
                        .loginPage("/customers/login")
                        .loginProcessingUrl("/customers/login")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/customers/login?error=invalidCredentials")
                        .usernameParameter("tenDangNhap")
                        .passwordParameter("matKhau")
                        .permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/customers/login")
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(new CustomOAuth2UserService(nguoiDungService, session))
                        )
                        .successHandler(oauth2SuccessHandler)
                )
                .logout(logout -> logout
                        .logoutUrl("/customers/logout")
                        .logoutSuccessUrl("/customers/login?logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(customerAuthEntryPoint())
                        .accessDeniedHandler(accessDeniedHandlerCustomers())
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionConcurrency(concurrency -> concurrency
                                .maximumSessions(1)
                                .expiredUrl("/customers/login?expired")
                        )
                        .invalidSessionUrl("/customers/login?invalid")
                )
                .csrf(csrf -> csrf.disable());

        return http.build();
    }

    @Bean
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        NguoiDungService nguoiDungService = applicationContext.getBean(NguoiDungService.class);
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/chitietsanpham", "/new", "/all", "/bestsellers", "/category/**",
                                "/search/**", "/polyshoe/login", // <- đã bỏ "/polyshoe/verify-face"
                                "/customers/login",
                                "/customers/oauth2/register", "/api/cart/check-auth", "/api/cart/get-user",
                                "/css/**", "/js/**", "/image/**", "/images/**", "/vi/**", "/uploads/**",
                                "/ws/**"
                        ).permitAll()
                        .requestMatchers("/cart", "/api/cart/**").authenticated()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/customers/login")
                        .loginProcessingUrl("/customers/login")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/customers/login?error=invalidCredentials")
                        .usernameParameter("tenDangNhap")
                        .passwordParameter("matKhau")
                        .permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/customers/login")
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(new CustomOAuth2UserService(nguoiDungService, session))
                        )
                        .successHandler(oauth2SuccessHandler)
                )
                .logout(logout -> logout
                        .logoutUrl("/customers/logout")
                        .logoutSuccessUrl("/customers/login?logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(defaultAuthEntryPoint())
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
                        .sessionConcurrency(concurrency -> concurrency
                                .maximumSessions(1)
                                .expiredUrl("/customers/login?expired")
                        )
                        .invalidSessionUrl("/customers/login?invalid")
                )
                .csrf(csrf -> csrf.disable());
        return http.build();
    }


    @Bean
    public DaoAuthenticationProvider authenticationProvider(CustomUserDetailsService userDetailsService,
                                                            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider auth = new DaoAuthenticationProvider();
        auth.setUserDetailsService(userDetailsService);
        auth.setPasswordEncoder(passwordEncoder);
        return auth;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}