package searchengine.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import searchengine.dto.ResponseMessageDTO;

@ControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(UnknownUrlPathException.class)
    protected ResponseEntity<ResponseMessageDTO> handlePropertyValueException(UnknownUrlPathException e) {
        return new ResponseEntity<>(ResponseMessageDTO.builder()
                .result(false)
                .error(e.getMessage())
                .build(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IndexingServiceException.class)
    protected ResponseEntity<ResponseMessageDTO> handleIndexingServiceException(IndexingServiceException e) {
        return new ResponseEntity<>(ResponseMessageDTO.builder()
                .result(false)
                .error(e.getMessage())
                .build(), HttpStatus.BAD_REQUEST);
    }
}
