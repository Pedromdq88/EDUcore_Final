package com.educore.sge.auth;

import com.educore.sge.institution.infrastructure.entity.StaffJpaEntity;
import com.educore.sge.institution.infrastructure.repository.StaffJpaRepository;
import com.educore.sge.institution.infrastructure.repository.PortfolioJpaRepository;
import com.educore.sge.shared.Rol;
import com.educore.sge.shared.exception.ApiErrorAdvice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MyStaffProfileRoutesTest {
    private final AuthUserRepository accounts = mock(AuthUserRepository.class);
    private final StaffJpaRepository staff = mock(StaffJpaRepository.class);
    private final PortfolioJpaRepository portfolios = mock(PortfolioJpaRepository.class);
    private final Authentication actor = new UsernamePasswordAuthenticationToken("ana@example.com", null, List.of());
    private StaffJpaEntity person;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        var account = new AuthUser();
        account.staffId = "staff";
        account.enabled = true;
        when(accounts.findByTenantIdAndEmailIgnoreCase("school", actor.getName())).thenReturn(Optional.of(account));
        person = new StaffJpaEntity();
        person.setId("staff");
        person.setTenantId("school");
        person.setFirstName("Ana");
        person.setLastName("Pérez");
        person.setRole(Rol.DIRECTOR);
        when(staff.findById("staff")).thenReturn(Optional.of(person));
        mvc = MockMvcBuilders.standaloneSetup(new MyStaffProfileController(accounts, staff),
            new MyStaffPortfolioController(portfolios, accounts, staff))
            .setControllerAdvice(new ApiErrorAdvice()).build();
    }

    @Test
    void profileRouteReturnsStaffInsteadOf404() throws Exception {
        mvc.perform(get("/api/v1/profile/staff").header("X-Institution-Id", "school").principal(actor))
            .andExpect(status().isOk()).andExpect(jsonPath("$.firstName").value("Ana"));
    }

    @Test
    void portfolioRouteExists() throws Exception {
        when(portfolios.findAllByTenantIdAndStaffIdOrderByActivityDateDesc("school", "staff")).thenReturn(List.of());
        mvc.perform(get("/api/v1/profile/staff/portfolios").header("X-Institution-Id", "school").principal(actor))
            .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void anotherInstitutionCannotReadTheProfile() throws Exception {
        person.setTenantId("another-school");
        mvc.perform(get("/api/v1/profile/staff").header("X-Institution-Id", "school").principal(actor))
            .andExpect(status().isForbidden());
    }
}
