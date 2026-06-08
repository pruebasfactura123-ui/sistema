package com.example.sistema.repository;

import com.example.sistema.model.Nomina;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NominaRepository extends JpaRepository<Nomina, Long> {
    // Busca todas las nóminas ordenadas por la fecha de emisión más reciente
    List<Nomina> findAllByOrderByFechaEmisionDesc();
}