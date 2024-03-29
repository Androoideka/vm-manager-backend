package io.github.androoideka.vm_manager.services;

import org.springframework.data.jpa.domain.Specification;

import io.github.androoideka.vm_manager.model.Machine;
import io.github.androoideka.vm_manager.model.Status;
import io.github.androoideka.vm_manager.model.User;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class MachineSpecification implements Specification<Machine> {

    private final String name;
    private final List<Status> statuses;
    private final LocalDate dateFrom;
    private final LocalDate dateTo;
    private final Long userId;
    private boolean showInactive;

    public MachineSpecification(String name, List<String> statuses, LocalDate dateFrom, LocalDate dateTo, Long userId) {
        this.name = name;
        if (statuses.size() == 1 && statuses.get(0).equals("NONE")) {
            this.statuses = new ArrayList<>();
        } else {
            this.statuses = statuses.stream().map(Status::valueOf).collect(Collectors.toList());
        }
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
        this.userId = userId;
        this.showInactive = false;
    }

    public void setShowInactive(boolean value) {
        this.showInactive = value;
    }

    @Override
    public Predicate toPredicate(Root<Machine> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
        List<Predicate> predicates = new ArrayList<>();
        // Status filter
        Predicate statusPredicate = null;
        for (Status status : statuses) {
            Predicate currentPredicate = criteriaBuilder.equal(root.get("status"), status);
            if (statusPredicate == null) {
                statusPredicate = currentPredicate;
            } else {
                statusPredicate = criteriaBuilder.or(statusPredicate, currentPredicate);
            }
        }
        if (statusPredicate != null) {
            predicates.add(statusPredicate);
        } else {
            return criteriaBuilder.disjunction();
        }
        // Admin filters (not in use)
        if (!showInactive) {
            predicates.add(criteriaBuilder.equal(root.get("active"), true));
        }
        if (userId != -1) {
            Join<User, Machine> userOfMachines = root.join("createdBy");
            predicates.add(criteriaBuilder.equal(userOfMachines.get("userId"), userId));
        }
        // Name filter
        if (!name.isBlank()) {
            predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("name")),
                    "%" + name.toLowerCase(Locale.ROOT) + "%"));
        }
        // Date filter
        if (dateFrom != null && dateTo != null) {
            predicates.add(criteriaBuilder.between(root.get("created"),
                    dateFrom.atStartOfDay(),
                    dateTo.atTime(LocalTime.MAX)));
        }
        // Combination
        return predicates.stream()
                .reduce(predicates.get(0),
                        criteriaBuilder::and);
    }
}
