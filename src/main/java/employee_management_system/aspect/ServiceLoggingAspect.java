package employee_management_system.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect // This annotation indicates that this class is an aspect
@Component // This annotation indicates that this class is a component
public class ServiceLoggingAspect {
    
    @Around ("execution(* employee_management_system.service.*.*(..))")
    public Object logAroundServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        // Log the method being called
        System.out.println("Calling method: " + joinPoint.getSignature().getName());
        Long startTime = System.currentTimeMillis();
        
        // Log the arguments being passed to the method
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            System.out.println("Argument: " + arg);
        }

        try {
            // Call the method
            Object result = joinPoint.proceed();
            Long endTime = System.currentTimeMillis();
            System.out.println("Method " + joinPoint.getSignature().getName() + " took " + (endTime - startTime) + "ms");
            return result;
        } catch (Throwable throwable) {
            // Log the error
            System.out.println("Error: " + throwable.getMessage());
            throw throwable;
        } 
    }
}
