package com.example.sistema.repository;

import com.example.sistema.model.Asistencia;
import com.example.sistema.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AsistenciaRepository extends JpaRepository<Asistencia, Long> {

    // Recupera todo el historial de asistencias de un empleado en específico
    List<Asistencia> findByUsuario(Usuario usuario);

    // Busca si el usuario ya tiene un registro de asistencia en la fecha actual
    Optional<Asistencia> findByUsuarioAndFecha(Usuario usuario, LocalDate fecha);
}