package Fifan.t.Egoisu.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

/**
 * Red de seguridad: convierte excepciones de negocio que no se capturaron en el Controller
 * en páginas de error amigables (sin mostrar trazas internas).
 * No captura Exception genérica a propósito: así no interfiere con AccessDeniedException de Spring Security.
 */
@ControllerAdvice
public class ManejadorExcepciones {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ModelAndView noEncontrado(ResourceNotFoundException e) {
        return vista("error/404", HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(UnauthorizedActionException.class)
    public ModelAndView noAutorizado(UnauthorizedActionException e) {
        return vista("error/403", HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ModelAndView reglaNegocio(ReglaNegocioException e) {
        return vista("error", HttpStatus.BAD_REQUEST, e.getMessage());
    }

    private ModelAndView vista(String nombre, HttpStatus estado, String mensaje) {
        ModelAndView mav = new ModelAndView(nombre);
        mav.setStatus(estado);
        mav.addObject("mensaje", mensaje);
        mav.addObject("estado", estado.value());
        return mav;
    }
}
