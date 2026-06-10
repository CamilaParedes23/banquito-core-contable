# Estándar de codificación aplicado

- Paquete base: `com.banquito.core.accounting`.
- Entidades JPA con `@Entity`, `@Table`, `@Id`, `@GeneratedValue`, `@Column` explícito.
- Imports Jakarta (`jakarta.persistence.*`).
- Lombok permitido solo para `@Getter`, `@Setter` y `@RequiredArgsConstructor` en servicios/controladores.
- No se usa `@Data` en entidades.
- Enums con sufijo `Enum` y `@Enumerated(EnumType.STRING)`.
- `BigDecimal` para montos y saldos.
- `LocalDate`, `LocalDateTime`, `LocalTime` para fechas y horas.
- `@Version` en entidades mutables.
- `equals()` y `hashCode()` manuales por ID.
- `toString()` manual y seguro.
- Repositorios como interfaces `JpaRepository`.
- Controladores sin reglas de negocio; delegan a servicios.
- No hay relaciones JPA entre microservicios.
