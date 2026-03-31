package com.inventario.controller;

import com.inventario.dto.request.ProductoCreateRequest;
import com.inventario.dto.request.ProductoUpdateRequest;
import com.inventario.dto.response.ProductoResponse;
import com.inventario.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la gestión de productos.
 * Proporciona endpoints para obtener, crear, actualizar, eliminar y buscar productos.
 * Requiere autenticación y autorización según el endpoint.
 */
@RestController
@RequestMapping("/productos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Productos", description = "Endpoints para gestionar productos del sistema")
public class ProductoController {

    private final ProductoService productoService;

    /**
     * Obtiene todos los productos activos.
     *
     * @return Lista de productos con estado 200 OK
     */
    @GetMapping
    @Operation(summary = "Obtener todos los productos",
            description = "Retorna la lista de todos los productos activos del sistema")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de productos obtenida exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public ResponseEntity<List<ProductoResponse>> getAllProductos() {
        log.info("GET /productos - Obteniendo todos los productos");
        List<ProductoResponse> productos = productoService.getAllProducts();
        return ResponseEntity.ok(productos);
    }

    /**
     * Obtiene un producto específico por su ID.
     *
     * @param id ID del producto a obtener
     * @return Producto con estado 200 OK o 404 Not Found si no existe
     */
    @GetMapping("/{id}")
    @Operation(summary = "Obtener producto por ID",
            description = "Retorna los detalles de un producto específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Producto obtenido exitosamente"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado"),
            @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public ResponseEntity<ProductoResponse> getProductoById(
            @Parameter(description = "ID del producto", required = true)
            @PathVariable Integer id) {
        log.info("GET /productos/{} - Obteniendo producto por ID", id);
        ProductoResponse producto = productoService.getProductById(id);
        return ResponseEntity.ok(producto);
    }

    /**
     * Busca productos por palabra clave en el nombre.
     *
     * @param keyword Palabra clave para buscar
     * @return Lista de productos que coinciden con la búsqueda
     */
    @GetMapping("/buscar")
    @Operation(summary = "Buscar productos",
            description = "Busca productos por palabra clave en el nombre (búsqueda insensible a mayúsculas)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Búsqueda realizada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Parámetro keyword es requerido"),
            @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public ResponseEntity<List<ProductoResponse>> searchProductos(
            @Parameter(description = "Palabra clave para buscar", required = true)
            @RequestParam String keyword) {
        log.info("GET /productos/buscar?keyword={} - Buscando productos", keyword);
        List<ProductoResponse> productos = productoService.searchProducts(keyword);
        return ResponseEntity.ok(productos);
    }

    /**
     * Crea un nuevo producto.
     * Requiere rol ADMIN.
     *
     * @param request DTO con los datos del nuevo producto
     * @return Producto creado con estado 201 Created
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crear nuevo producto",
            description = "Crea un nuevo producto en el sistema. Requiere rol ADMIN")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Producto creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o SKU duplicado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado - se requiere rol ADMIN"),
            @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public ResponseEntity<ProductoResponse> createProducto(
            @Valid @RequestBody ProductoCreateRequest request) {
        log.info("POST /productos - Creando nuevo producto: {}", request.nombre());
        ProductoResponse producto = productoService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(producto);
    }

    /**
     * Actualiza un producto existente.
     * Requiere rol ADMIN.
     *
     * @param id ID del producto a actualizar
     * @param request DTO con los datos actualizados
     * @return Producto actualizado con estado 200 OK
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar producto",
            description = "Actualiza los datos de un producto existente. Requiere rol ADMIN")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Producto actualizado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado - se requiere rol ADMIN"),
            @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public ResponseEntity<ProductoResponse> updateProducto(
            @Parameter(description = "ID del producto", required = true)
            @PathVariable Integer id,
            @Valid @RequestBody ProductoUpdateRequest request) {
        log.info("PUT /productos/{} - Actualizando producto", id);
        ProductoResponse producto = productoService.updateProduct(id, request);
        return ResponseEntity.ok(producto);
    }

    /**
     * Elimina (desactiva) un producto.
     * Requiere rol ADMIN.
     *
     * @param id ID del producto a eliminar
     * @return Estado 204 No Content si se elimina exitosamente
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Eliminar producto",
            description = "Desactiva un producto del sistema. Requiere rol ADMIN")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Producto eliminado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado"),
            @ApiResponse(responseCode = "403", description = "Acceso denegado - se requiere rol ADMIN"),
            @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public ResponseEntity<Void> deleteProducto(
            @Parameter(description = "ID del producto", required = true)
            @PathVariable Integer id) {
        log.info("DELETE /productos/{} - Eliminando producto", id);
        productoService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
