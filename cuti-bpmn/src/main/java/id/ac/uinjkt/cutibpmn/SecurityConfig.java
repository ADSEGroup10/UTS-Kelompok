package id.ac.uinjkt.cutibpmn;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpStatus;

/** Demo authentication with two roles; credentials can be overridden in application.properties. */
@Configuration
public class SecurityConfig {

    @Bean
    public InMemoryUserDetailsManager users(
            @Value("${app.security.karyawan.username:budi}") String karyawanUsername,
            @Value("${app.security.karyawan.password:budi123}") String karyawanPassword,
            @Value("${app.security.atasan.username:atasan}") String atasanUsername,
            @Value("${app.security.atasan.password:atasan123}") String atasanPassword) {
        UserDetails karyawan = User.withUsername(karyawanUsername)
                .password("{noop}" + karyawanPassword)
                .roles("KARYAWAN")
                .build();
        UserDetails atasan = User.withUsername(atasanUsername)
                .password("{noop}" + atasanPassword)
                .roles("ATASAN")
                .build();
        return new InMemoryUserDetailsManager(karyawan, atasan);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // This small demo has no CSRF token endpoint; all state-changing UI requests
                // use the same-origin session and the application does not expose cross-origin forms.
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/index.html", "/login", "/logout", "/error").permitAll()
                        .requestMatchers("/api/me").authenticated()
                        .requestMatchers("/api/cuti/**").hasRole("KARYAWAN")
                        .requestMatchers("/api/tugas/**").hasRole("ATASAN")
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                new AntPathRequestMatcher("/api/**")))
                .formLogin(form -> form
                        .loginPage("/")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/?login=error")
                        .permitAll())
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "POST"))
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessUrl("/?logout=success")
                        .permitAll());
        return http.build();
    }
}
