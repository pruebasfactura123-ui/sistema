package com.example.sistema.repository;

import com.example.sistema.model.Nomina;
import com.example.sistema.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NominaRepository extends JpaRepository<Nomina, Long> {

    // 1. Mantener tu consulta original por si la usas en algún reporte global de desarrollo
    List<Nomina> findAllByOrderByFechaEmisionDesc();

    // 2. FILTRADO PARA ADMINISTRADORES (JEFE/GERENTE):
    // Busca las nóminas de los trabajadores que pertenezcan a una empresa específica
    @Query("SELECT n FROM Nomina n WHERE n.trabajador.empresa.id = :empresaId ORDER BY n.fechaEmision DESC")
    List<Nomina> findByTrabajadorEmpresaIdOrderByFechaEmisionDesc(@Param("empresaId") Long empresaId);

    // 3. FILTRADO PARA EMPLEADOS:
    // Por si un empleado común entra a ver su historial, solo verá sus propios recibos
    List<Nomina> findByTrabajadorOrderByFechaEmisionDesc(Usuario trabajador);
}