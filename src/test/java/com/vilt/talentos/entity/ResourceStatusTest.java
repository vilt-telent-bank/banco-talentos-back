package com.vilt.talentos.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResourceStatusTest {

    @Test
    void fromRegistrationStatus_shouldFollowRn003Rules() {
        assertEquals(ResourceStatus.AVAILABLE, ResourceStatus.fromRegistrationStatus(null));
        assertEquals(ResourceStatus.AVAILABLE, ResourceStatus.fromRegistrationStatus(RegistrationStatus.NOT_REQUIRED));
        assertEquals(ResourceStatus.WAITING, ResourceStatus.fromRegistrationStatus(RegistrationStatus.REQUESTED_VIA_TICKET));
        assertEquals(ResourceStatus.WAITING, ResourceStatus.fromRegistrationStatus(RegistrationStatus.TICKET_AWAITING_APPROVAL));
        assertEquals(ResourceStatus.WAITING, ResourceStatus.fromRegistrationStatus(RegistrationStatus.TICKET_AWAITING_SERVICE));
        assertEquals(ResourceStatus.ALLOCATED, ResourceStatus.fromRegistrationStatus(RegistrationStatus.RELEASED));
    }
}
