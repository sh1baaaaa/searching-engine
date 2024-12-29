package searchengine.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import searchengine.dto.IndexingResponse;

@ControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(UnknownUrlPathException.class)
    protected ResponseEntity<IndexingResponse> handlePropertyValueException(UnknownUrlPathException e) {
        return new ResponseEntity<>(IndexingResponse.builder()
                .result(false)
                .error(e.getMessage())
                .build(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IndexingServiceException.class)
    protected ResponseEntity<IndexingResponse> handleIndexingServiceException(IndexingServiceException e) {
        return new ResponseEntity<>(IndexingResponse.builder()
                .result(false)
                .error(e.getMessage())
                .build(), HttpStatus.BAD_REQUEST);
    }
}
