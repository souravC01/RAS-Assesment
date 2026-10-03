package ca.ras.safety.photo;

import ca.ras.safety.TestcontainersConfiguration;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @Import(TestcontainersConfiguration.class)
@ActiveProfiles("storage-probe")
class StorageProbeTest {
    @Autowired MockMvc mvc;
    @MockitoBean PhotoStorage storage;
    private MockMultipartFile jpeg() throws Exception {
        var out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB), "jpeg", out);
        return new MockMultipartFile("photos", "photo.jpg", "image/jpeg", out.toByteArray());
    }
    @Test @WithMockUser(roles="ADMIN") void validatesBeforeUploading() throws Exception {
        when(storage.presign(anyString(), any())).thenReturn(URI.create("https://example.test/private"));
        mvc.perform(multipart("/api/admin/storage-probe").file(jpeg()).with(csrf()))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].url").value("https://example.test/private"));
        verify(storage).put(startsWith("probe/"), any(byte[].class), eq("image/jpeg"));
        clearInvocations(storage);
        mvc.perform(multipart("/api/admin/storage-probe").with(csrf())).andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/admin/storage-probe").file(new MockMultipartFile("photos", "fake.jpg", "image/jpeg", new byte[]{1,2})).with(csrf()))
            .andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/admin/storage-probe").file(new MockMultipartFile("photos", "large.jpg", "image/jpeg", new byte[5_000_001])).with(csrf()))
            .andExpect(status().isPayloadTooLarge());
        var six = multipart("/api/admin/storage-probe").with(csrf());
        for (int i=0;i<6;i++) six.file(jpeg());
        mvc.perform(six).andExpect(status().isBadRequest());
        verifyNoInteractions(storage);
        mvc.perform(delete("/api/admin/storage-probe").contentType("application/json")
            .content("[\"submissions/other.jpg\"]").with(csrf())).andExpect(status().isBadRequest());
        verifyNoInteractions(storage);
    }
    @Test @WithMockUser(roles="FRAMER") void workerCannotUploadProbe() throws Exception {
        mvc.perform(multipart("/api/admin/storage-probe").file(jpeg()).with(csrf())).andExpect(status().isForbidden());
        verifyNoInteractions(storage);
    }
    @Test @WithMockUser(roles="ADMIN") void csrfIsRequired() throws Exception {
        mvc.perform(multipart("/api/admin/storage-probe").file(jpeg())).andExpect(status().isForbidden());
        verifyNoInteractions(storage);
    }
}
