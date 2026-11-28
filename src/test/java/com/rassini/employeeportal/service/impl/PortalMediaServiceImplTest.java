package com.rassini.employeeportal.service.impl;

import com.rassini.employeeportal.config.PortalMediaProperties;
import com.rassini.employeeportal.dto.response.PortalMediaResponse;
import com.rassini.employeeportal.entity.PortalMediaEntity;
import com.rassini.employeeportal.entity.UserEntity;
import com.rassini.employeeportal.exception.BusinessException;
import com.rassini.employeeportal.exception.ResourceNotFoundException;
import com.rassini.employeeportal.mapper.PortalContentMapper;
import com.rassini.employeeportal.repository.PortalMediaRepository;
import com.rassini.employeeportal.repository.UserRepository;
import com.rassini.employeeportal.service.PortalMediaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class PortalMediaServiceImplTest {

    @TempDir
    Path tempDir;

    @Mock
    private PortalMediaRepository mediaRepository;
    @Mock
    private UserRepository userRepository;

    private PortalMediaServiceImpl service;

    private static final byte[] VALID_PNG = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00};
    private static final byte[] VALID_JPG = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00};
    private static final byte[] VALID_WEBP = new byte[]{'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P', 0};

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        PortalMediaProperties props = new PortalMediaProperties();
        props.setPath(tempDir.toString());
        props.setMaxSizeBytes(1024);
        service = new PortalMediaServiceImpl(props, mediaRepository, userRepository, new PortalContentMapper());
    }

    @Test
    void upload_validPngJpgWebp_succeedsAndStoresWithUuid() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(UserEntity.builder().id(1L).username("comms").build()));
        when(mediaRepository.save(any())).thenAnswer(inv -> {
            PortalMediaEntity e = inv.getArgument(0);
            e.setId(10L);
            return e;
        });

        MockMultipartFile png = new MockMultipartFile("file", "banner.png", "image/png", VALID_PNG);
        PortalMediaResponse resPng = service.upload(png, 1L);
        assertEquals(10L, resPng.getId());
        assertEquals("image/png", resPng.getContentType());

        MockMultipartFile jpg = new MockMultipartFile("file", "photo.jpeg", "image/jpg", VALID_JPG);
        PortalMediaResponse resJpg = service.upload(jpg, null);
        assertEquals("image/jpeg", resJpg.getContentType());

        MockMultipartFile jpgNoMime = new MockMultipartFile("file", "photo2.jpg", "", VALID_JPG);
        assertEquals("image/jpeg", service.upload(jpgNoMime, null).getContentType());

        MockMultipartFile webp = new MockMultipartFile("file", "hero.webp", "application/octet-stream", VALID_WEBP);
        PortalMediaResponse resWebp = service.upload(webp, null);
        assertEquals("image/webp", resWebp.getContentType());

        assertThrows(ResponseStatusException.class,
                () -> service.upload(new MockMultipartFile("file", "badmime.png", "image/jpeg", VALID_PNG), null));
        assertThrows(ResponseStatusException.class,
                () -> service.upload(new MockMultipartFile("file", "short.png", "image/png", new byte[]{1, 2}), null));
    }

    @Test
    void upload_invalidCases_throwExpectedExceptions() {
        assertThrows(BusinessException.class, () -> service.upload(null, 1L));
        assertThrows(BusinessException.class, () -> service.upload(new MockMultipartFile("file", "a.png", "image/png", new byte[0]), 1L));

        byte[] tooBig = new byte[2048];
        System.arraycopy(VALID_PNG, 0, tooBig, 0, VALID_PNG.length);
        ResponseStatusException bigEx = assertThrows(ResponseStatusException.class,
                () -> service.upload(new MockMultipartFile("file", "big.png", "image/png", tooBig), 1L));
        assertEquals(413, bigEx.getStatusCode().value());

        ResponseStatusException svgEx = assertThrows(ResponseStatusException.class,
                () -> service.upload(new MockMultipartFile("file", "x.svg", "image/svg+xml", "<svg/>".getBytes()), 1L));
        assertEquals(415, svgEx.getStatusCode().value());

        ResponseStatusException fakePng = assertThrows(ResponseStatusException.class,
                () -> service.upload(new MockMultipartFile("file", "fake.png", "image/png", "not-a-png".getBytes()), 1L));
        assertEquals(415, fakePng.getStatusCode().value());

        ResponseStatusException mismatch = assertThrows(ResponseStatusException.class,
                () -> service.upload(new MockMultipartFile("file", "wrong.jpg", "image/jpeg", VALID_PNG), 1L));
        assertEquals(415, mismatch.getStatusCode().value());

        assertThrows(BusinessException.class,
                () -> service.upload(new MockMultipartFile("file", "   ", "image/png", VALID_PNG), 1L));
        assertThrows(ResponseStatusException.class,
                () -> service.upload(new MockMultipartFile("file", "noext", "image/png", VALID_PNG), 1L));
    }

    @Test
    void load_validAndTraversalAndMissing() throws Exception {
        String key = "123e4567-e89b-12d3-a456-426614174000.png";
        Files.write(tempDir.resolve(key), VALID_PNG);
        PortalMediaEntity entity = PortalMediaEntity.builder()
                .id(1L).storageKey(key).originalName("a.png").contentType("image/png").sizeBytes((long) VALID_PNG.length).build();
        when(mediaRepository.findById(1L)).thenReturn(Optional.of(entity));

        PortalMediaService.MediaResource loaded = service.load(1L);
        assertTrue(loaded.resource().exists());
        assertEquals("image/png", loaded.metadata().getContentType());

        PortalMediaEntity badKey = PortalMediaEntity.builder().id(2L).storageKey("../secret.png").build();
        when(mediaRepository.findById(2L)).thenReturn(Optional.of(badKey));
        assertThrows(BusinessException.class, () -> service.load(2L));

        PortalMediaEntity missingFile = PortalMediaEntity.builder().id(3L).storageKey("123e4567-e89b-12d3-a456-426614174099.png").build();
        when(mediaRepository.findById(3L)).thenReturn(Optional.of(missingFile));
        assertThrows(ResourceNotFoundException.class, () -> service.load(3L));

        when(mediaRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.load(99L));
    }
}
