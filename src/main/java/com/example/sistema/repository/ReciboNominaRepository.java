package com.example.sistema.repository;

import com.example.sistema.model.ReciboNomina; // Asegúrate de que apunte a tu modelo real
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReciboNominaRepository extends JpaRepository<ReciboNomina, Long> {
    
    // Busca los recibos del trabajador logueado ordenados por la fecha más reciente
    List<ReciboNomina> findByUsuarioUsernameOrderByFechaPagoDesc(String username);
}