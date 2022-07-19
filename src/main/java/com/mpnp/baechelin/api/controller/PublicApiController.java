package com.mpnp.baechelin.api.controller;

import com.mpnp.baechelin.api.dto.PublicApiRequestDto;
import com.mpnp.baechelin.api.dto.PublicApiResponseDto;
import com.mpnp.baechelin.api.service.PublicApiService;
import com.mpnp.baechelin.store.dto.StoreCardResponseDto;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

import static java.lang.System.currentTimeMillis;

@RestController
@RequiredArgsConstructor
@Slf4j
@ApiOperation(value="공공 API에 접근해 정보를 받아오는 컨트롤러")
public class PublicApiController {
    private final PublicApiService publicApiService;
    @ApiOperation(value="공공 API V1을 통해 DB에 정보를 저장/업데이트하는 함수")
    @PostMapping("/api")
    public ResponseEntity<String> findPublicApi(@RequestBody PublicApiRequestDto publicApiRequestDto) throws IOException {
        long start = currentTimeMillis();
        publicApiService.processApiToDBWithRestTemplate(publicApiRequestDto);
//        ResponseEntity<PublicApiResponseDto> result = ResponseEntity.ok(publicApiService.processApiToDBWithWebclientMono(publicApiRequestDto));
        log.info("Elapsed Time : {}", currentTimeMillis() - start);
        return ResponseEntity.ok("공공 API 저장 완료");
    }
    //TODO 정리하기
    @ApiOperation(value="공공 API V2을 통해 DB에 정보를 저장/업데이트하는 함수")
    @GetMapping("/new-api")
    public ResponseEntity<String> findNewPublicApi(@RequestParam String serviceKey,
                                                          @RequestParam int rslSize,
                                                          @RequestParam String siDoNm){
        publicApiService.processNewApi(serviceKey, rslSize, siDoNm);
        return ResponseEntity.ok("공공 API V2 저장 완료");
    }

}
