package com.mlkzdh.quicklink.redirect.controller;

import java.net.URI;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import com.mlkzdh.quicklink.redirect.controller.model.UrlRecord;
import com.mlkzdh.quicklink.redirect.db.model.HitRecord;
import com.mlkzdh.quicklink.redirect.service.RedirectService;
import com.mlkzdh.quicklink.util.Base62;
import jakarta.servlet.http.HttpServletRequest;

@RestController
public final class RedirectController {

  private final RedirectService redirectService;

  @Autowired
  public RedirectController(RedirectService redirectService) {
    this.redirectService = redirectService;
  }

  /**
   * Looks up the destination URL for the given key, saves the hit record in the database, and
   * redirects to the destination URL
   * 
   * @param key The key associated wit the destination URL
   * @param request The original HTTP request
   * @return The HTTP response that redirects to the destination URL
   * @throws ResponseStatusException When the key does not exist in the database
   */
  @GetMapping("/u/{key}")
  public ResponseEntity<Void> redirect(@PathVariable String key, HttpServletRequest request)
      throws ResponseStatusException {
    // Validation
    // Lookup
    Optional<UrlRecord> urlRecord = redirectService.findUrlRecord(Base62.toBase10(key));
    if (urlRecord.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    // Persistence
    HitRecord hitRecord = buildHitRecord(urlRecord.get(), request);
    redirectService.save(hitRecord);
    // Response
    HttpHeaders headers = new HttpHeaders();
    headers.setCacheControl("no-cache");
    headers.setLocation(URI.create(urlRecord.get().getDestination()));
    return new ResponseEntity<>(headers, HttpStatus.MOVED_PERMANENTLY);
  }

  private static HitRecord buildHitRecord(UrlRecord urlRecord,
      HttpServletRequest request) {
    return new HitRecord.Builder()
        .urlRecordId(urlRecord.getId())
        .ip(request.getRemoteAddr())
        .userAgent(request.getHeader(HttpHeaders.USER_AGENT))
        .referer(request.getHeader(HttpHeaders.REFERER))
        .build();
  }

}
