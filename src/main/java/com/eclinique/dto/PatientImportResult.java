package com.eclinique.dto;

import java.util.List;

public record PatientImportResult(int importes, int ignores, List<String> erreurs) {
}