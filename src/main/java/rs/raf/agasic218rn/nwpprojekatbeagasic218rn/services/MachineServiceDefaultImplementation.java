package rs.raf.agasic218rn.nwpprojekatbeagasic218rn.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import rs.raf.agasic218rn.nwpprojekatbeagasic218rn.exceptions.ConcurrentOperationException;
import rs.raf.agasic218rn.nwpprojekatbeagasic218rn.exceptions.ErrorCause;
import rs.raf.agasic218rn.nwpprojekatbeagasic218rn.exceptions.InvalidMachineStateException;
import rs.raf.agasic218rn.nwpprojekatbeagasic218rn.mappers.MachineMapper;
import rs.raf.agasic218rn.nwpprojekatbeagasic218rn.model.*;
import rs.raf.agasic218rn.nwpprojekatbeagasic218rn.repositories.ErrorRepository;
import rs.raf.agasic218rn.nwpprojekatbeagasic218rn.repositories.MachineRepository;
import rs.raf.agasic218rn.nwpprojekatbeagasic218rn.requests.MachineRequest;
import rs.raf.agasic218rn.nwpprojekatbeagasic218rn.responses.MachineResponse;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MachineServiceDefaultImplementation implements MachineService {
    private static final long TIME_INCREMENT = 5000;
    private final MachineMapper machineMapper;
    private final MachineRepository machineRepository;
    private final ErrorRepository errorRepository;
    private final UserService userService;
    private final TaskScheduler taskScheduler;

    @Autowired
    public MachineServiceDefaultImplementation(MachineMapper machineMapper, MachineRepository machineRepository, ErrorRepository errorRepository, UserService userService, TaskScheduler taskScheduler) {
        this.machineMapper = machineMapper;
        this.machineRepository = machineRepository;
        this.errorRepository = errorRepository;
        this.userService = userService;
        this.taskScheduler = taskScheduler;
    }

    @Override
    public MachineResponse create(MachineRequest machineRequest) {
        Machine machine = this.machineMapper.machineRequestToMachine(machineRequest, userService.getCurrentUser());
        machine = this.machineRepository.save(machine);
        return this.machineMapper.machineToMachineResponse(machine);
    }

    @Override
    public Page<MachineResponse> search(String name, List<String> statuses, LocalDate dateFrom, LocalDate dateTo, Integer page, Integer size) {
        List<Status> statusesEnum = statuses.stream().map(Status::valueOf).collect(Collectors.toList());
        MachineSpecification specification = new MachineSpecification(name, statusesEnum, dateFrom, dateTo, userService.getCurrentUser().getUserId());
        return this.machineRepository.findAll(specification, PageRequest.of(page, size))
                .map(this.machineMapper::machineToMachineResponse);
    }

    @Override
    public void executeOperation(Long machineId, MachineOperation machineOperation) {
        Machine machine = this.get(machineId);
        // Initiate checks
        if(!this.isValidState(machine, machineOperation)) {
            throw new InvalidMachineStateException(
                    ErrorCause.generateMessage(ErrorCause.INVALID_STATE, machineOperation, machine.getStatus()), machine.getStatus());
        }
        if(!this.isReadyToExecute(machine, machineOperation)) {
            throw new ConcurrentOperationException(ErrorCause.generateMessage(ErrorCause.CONCURRENCY, machineOperation, machine.getStatus()));
        }
        // Refresh version and opCounter
        machine = this.machineRepository
                .getById(machineId);
        commenceOperation(machine, machineOperation);
    }

    @Override
    public void scheduleOperation(Long machineId, MachineOperation machineOperation, String cron) {
        this.get(machineId);
        CronTrigger cronTrigger = new CronTrigger(cron);
        this.taskScheduler.schedule(() -> {
            // Check if machine still exists
            Optional<Machine> machineOptional = this.machineRepository
                    .findById(machineId);
            if(machineOptional.isPresent()) {
                Machine machineCurr = machineOptional.get();
                if(this.isValidState(machineCurr, machineOperation)) {
                    // Refresh version and opCounter
                    machineCurr = this.machineRepository
                            .getById(machineId);
                    if(this.isReadyToExecute(machineCurr, machineOperation)) {
                        commenceOperation(machineCurr, machineOperation);
                    }
                }
            }
        }, cronTrigger);
    }

    private Machine get(Long machineId) {
        Machine machine = this.machineRepository
                .findById(machineId)
                .orElseThrow(() -> new AccessDeniedException("This machine does not exist or does not belong to you."));
        if(!userService.getCurrentUser().getUserId().equals(machine.getCreatedBy().getUserId())) {
            throw new AccessDeniedException("This machine does not exist or does not belong to you.");
        }
        return machine;
    }

    private boolean isValidState(Machine machine, MachineOperation machineOperation) {
        if(machineOperation == MachineOperation.START && machine.getStatus() == Status.STOPPED) {
            return true;
        }
        if(machineOperation == MachineOperation.STOP && machine.getStatus() == Status.RUNNING) {
            return true;
        }
        if(machineOperation == MachineOperation.RESTART && machine.getStatus() == Status.RUNNING) {
            return true;
        }
        ErrorLog errorLog = new ErrorLog(machineOperation, machine, ErrorCause.INVALID_STATE);
        this.errorRepository.save(errorLog);
        return false;
    }

    private boolean isReadyToExecute(Machine machine, MachineOperation machineOperation) {
        Integer modified = this.machineRepository.incrementOpCounter(machine.getMachineId());
        if(modified.equals(0)) {
            ErrorLog errorLog = new ErrorLog(machineOperation, machine, ErrorCause.CONCURRENCY);
            this.errorRepository.save(errorLog);
            return false;
        }
        return true;
    }

    private void commenceOperation(Machine machine, MachineOperation machineOperation) {
        if(machineOperation == MachineOperation.DESTROY) {
            machine.setActive(false);
            this.machineRepository.save(machine);
            return;
        }
        long randomDelay = (long) (Math.random() * TIME_INCREMENT);
        long totalDelay = randomDelay + TIME_INCREMENT;
        if(machineOperation != MachineOperation.RESTART) {
            totalDelay += TIME_INCREMENT;
        }
        this.taskScheduler.schedule(() ->
                finishOperation(machine, machineOperation), new Date(System.currentTimeMillis() + totalDelay));
    }

    private void finishOperation(Machine machine, MachineOperation machineOperation) {
        if(machineOperation == MachineOperation.START) {
            machine.setStatus(Status.RUNNING);
        } else if(machineOperation == MachineOperation.STOP) {
            machine.setStatus(Status.STOPPED);
        } else if(machineOperation == MachineOperation.RESTART) {
            machine.setStatus(Status.STOPPED);
        }
        machine = this.machineRepository.save(machine);
        if(machineOperation == MachineOperation.RESTART) {
            // Has to be modified one more time
            machine.setOpCounter(machine.getOpCounter() + 1);
            long randomDelay = (long) (Math.random() * TIME_INCREMENT);
            long totalDelay = randomDelay + TIME_INCREMENT;
            Machine finalMachine = machine;
            this.taskScheduler.schedule(() -> {
                finalMachine.setStatus(Status.RUNNING);
                this.machineRepository.save(finalMachine);
            }, new Date(System.currentTimeMillis() + totalDelay));
        }
    }

    @Override
    public void destroy(Long machineId) {
        Machine machine = this.get(machineId);
        MachineOperation machineOperation = MachineOperation.DESTROY;
        if(machine.getStatus() != Status.STOPPED) {
            throw new InvalidMachineStateException(
                    ErrorCause.generateMessage(ErrorCause.INVALID_STATE, machineOperation, machine.getStatus()), Status.RUNNING);
        }
        if(!this.isReadyToExecute(machine, MachineOperation.DESTROY)) {
            throw new ConcurrentOperationException(ErrorCause.generateMessage(ErrorCause.CONCURRENCY, machineOperation, machine.getStatus()));
        }
        machine.setActive(false);
        this.machineRepository.save(machine);
    }
}
