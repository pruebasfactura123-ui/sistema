package com.example.sistema.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @SuppressWarnings("deprecation")
    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        // Creamos un usuario maestro directamente en la memoria del servidor
        UserDetails admin = User.withUsername("admin")
                .password("admin123")
                .authorities("ROLE_JEFE", "JEFE")
                .build();
        return new InMemoryUserDetailsManager(admin);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // 1. Accesos públicos
                .requestMatchers("/login", "/registrar-empresa", "/css/**", "/js/**", "/images/**").permitAll()
                
                // 2. Permisos de Administración
                .requestMatchers("/usuarios/crear", "/borrar-todo", "/usuarios/cambiar-rol/**").hasAnyAuthority("JEFE", "ROLE_JEFE")
                
                // 3. Permisos de Gestión de Personal y Nóminas
                .requestMatchers(
                    "/usuarios/eliminar/**", 
                    "/usuarios",
                    "/operaciones/nominas",
                    "/operaciones/nominas/**"
                ).hasAnyAuthority("JEFE", "ROLE_JEFE", "GERENTE", "ROLE_GERENTE")
                
                // 4. Dashboard principal y operaciones comunes
                .requestMatchers(
                    "/", 
                    "/facturas/nueva", 
                    "/facturas/historial", 
                    "/subir", 
                    "/descargar/**", 
                    "/borrar/**", 
                    "/perfil", 
                    "/perfil/actualizar-clave", 
                    "/perfil/actualizar-foto"
                ).hasAnyAuthority("JEFE", "ROLE_JEFE", "GERENTE", "ROLE_GERENTE", "TRABAJADOR", "ROLE_TRABAJADOR")
                
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .permitAll()
            );
        
        return http.build();
    }
}