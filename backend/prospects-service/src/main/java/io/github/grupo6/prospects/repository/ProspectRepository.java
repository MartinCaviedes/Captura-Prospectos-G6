package io.github.grupo6.prospects.repository;

import io.github.grupo6.prospects.model.Prospect;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProspectRepository extends JpaRepository<Prospect, UUID>, JpaSpecificationExecutor<Prospect> {}
