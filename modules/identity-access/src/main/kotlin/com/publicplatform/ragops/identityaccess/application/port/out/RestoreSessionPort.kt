package com.publicplatform.ragops.identityaccess.application.port.out

import com.publicplatform.ragops.identityaccess.domain.AdminSessionSnapshot
import com.publicplatform.ragops.identityaccess.domain.SessionLookup

interface RestoreSessionPort {
    fun restoreSession(lookup: SessionLookup): AdminSessionSnapshot
}
