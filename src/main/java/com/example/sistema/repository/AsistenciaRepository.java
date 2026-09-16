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

    // Recupera todo el historial de asistencias de un empleado específico
    List<Asistencia> findByUsuario(Usuario usuario);

    // Busca si el usuario ya tiene un registro de asistencia en la fecha actual
    Optional<Asistencia> findByUsuarioAndFecha(Usuario usuario, LocalDate fecha);

    // Verifica si existe un registro de asistencia para un usuario en la fecha indicada
    boolean existsByUsuarioAndFecha(Usuario usuario, LocalDate fecha);

    // Obtiene la lista completa de asistencias de todos los empleados en una fecha específica (Pase de Lista del Día)
    List<Asistencia> findByFecha(LocalDate fecha);

    // Obtiene las asistencias en un rango de fechas (ideal para reportes semanales o mensuales)
    List<Asistencia> findByFechaBetween(LocalDate inicio, LocalDate fin);
}