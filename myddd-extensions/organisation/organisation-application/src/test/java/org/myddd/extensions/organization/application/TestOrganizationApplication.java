package org.myddd.extensions.organization.application;

import com.google.protobuf.Int64Value;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.myddd.extensions.organisation.NotAllowedDeleteTopOrganizationException;
import org.myddd.extensions.organisation.OrganizationNotExistsException;
import org.myddd.extensions.organisation.UserNotFoundException;
import org.myddd.extensions.organization.AbstractTest;
import org.myddd.extensions.organization.api.*;
import org.myddd.extensions.security.api.UserApplication;
import org.myddd.extensions.security.api.UserDto;

import javax.inject.Inject;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;

class TestOrganizationApplication extends AbstractTest {

    @Inject
    private EmployeeApplication employeeApplication;

    @Inject
    private OrganizationApplication organizationApplication;

    @Inject
    private UserApplication userApplication;

    @Test
    void testAssertOrganizationApplicationNotNull(){
        Assertions.assertNotNull(organizationApplication);
    }

    @BeforeEach
    void beforeEach(){
        Mockito.when(userApplication.queryUserById(any())).thenReturn(randomUserDto());
    }

    @Test
    void testCreateTopCompany(){
        OrganizationDto created = organizationApplication.createTopOrganization(randomOrganization());
        Assertions.assertNotNull(created);
    }

    @Test
    void testQueryOrganizationSystemManagerEmployees(){
        var notExistsOrgId = Int64Value.of(randomLong());
        Assertions.assertThrows(OrganizationNotExistsException.class,()->organizationApplication.queryOrganizationSystemManagerEmployees(notExistsOrgId));

        UserDto randomUserDto = randomUserDto();
        Mockito.when(userApplication.queryUserById(any())).thenReturn(randomUserDto);

        var created = organizationApplication.createTopOrganization(randomOrganizationWithUserId(randomUserDto.getId()));
        Assertions.assertFalse(organizationApplication.queryOrganizationSystemManagerEmployees(Int64Value.of(created.getId())).getEmployeesList().isEmpty());
    }


    @Test
    void testCreateDepartment(){
        OrganizationDto createdCompany = organizationApplication.createTopOrganization(randomOrganization());
        OrganizationDto createdDepartment = organizationApplication.createDepartment(randomDepartment(createdCompany));
        Assertions.assertNotNull(createdDepartment);
    }

    @Test
    void testUpdateOrganization(){
        OrganizationDto created = organizationApplication.createTopOrganization(randomOrganization());

        String newName = randomId();
        OrganizationDto willUpdate = OrganizationDto.newBuilder()
                .setId(created.getId())
                .setName(newName)
                .build();

        OrganizationDto updated = organizationApplication.updateOrganization(willUpdate);
        Assertions.assertEquals(newName,updated.getName());
    }

    @Test
    void testDeleteOrganization(){
        var createdCompany = organizationApplication.createTopOrganization(randomOrganization());
        var notAllowedDeleteOrgId = Int64Value.of(createdCompany.getId());
        Assertions.assertThrows(NotAllowedDeleteTopOrganizationException.class,()->organizationApplication.deleteOrganization(notAllowedDeleteOrgId));

        var createdDepartment = organizationApplication.createDepartment(randomDepartment(createdCompany));
        Assertions.assertDoesNotThrow(()-> organizationApplication.deleteOrganization(Int64Value.of(createdDepartment.getId())));
    }

    @Test
    void testJoinOrganization(){
        Mockito.when(userApplication.queryUserById(any())).thenReturn(null);
        var userNotFoundJoinDto = JoinOrLeaveOrganizationDto.newBuilder().setOrgId(-1L).setUserId(randomLong()).build();
        Assertions.assertThrows(UserNotFoundException.class,()->{organizationApplication.joinOrganization(userNotFoundJoinDto);});

        UserDto randomUserDto = randomUserDto();
        Mockito.when(userApplication.queryUserById(any())).thenReturn(randomUserDto);
        EmployeeDto createdEmployee = randomCreateEmployee();
        var organizationNotExistsOrgId = JoinOrLeaveOrganizationDto.newBuilder().setOrgId(-1L).setUserId(createdEmployee.getId()).build();
        Assertions.assertThrows(OrganizationNotExistsException.class,()-> organizationApplication.joinOrganization(organizationNotExistsOrgId));

        OrganizationDto created = organizationApplication.createTopOrganization(randomOrganization());
        boolean success = organizationApplication.joinOrganization(JoinOrLeaveOrganizationDto.newBuilder().setOrgId(created.getId()).setUserId(randomUserDto.getId()).build()).getValue();
        Assertions.assertTrue(success);
    }

    @Test
    void testQueryCompaniesByUserId(){
        EmployeeDto created = randomCreateEmployee();

        ListOrganizationDto listOrganizationDto = organizationApplication.queryTopCompaniesByUserId(Int64Value.of(created.getId()));
        Assertions.assertFalse(listOrganizationDto.getOrganizationsList().isEmpty());
    }

    @Test
    void testListCompanyTrees(){
        OrganizationDto createdCompany = organizationApplication.createTopOrganization(randomOrganization());
        organizationApplication.createDepartment(randomDepartment(createdCompany));

        var list = organizationApplication.listCompanyTrees(QueryCompanyTreeDto.newBuilder().setOrgId(createdCompany.getId()).build());
        Assertions.assertEquals(2,list.getOrganizationsList().size());

        var emptyList = organizationApplication.listCompanyTrees(QueryCompanyTreeDto.newBuilder()
                .setOrgId(createdCompany.getId())
                .addAllLimits(List.of(randomLong()))
                .build());
        Assertions.assertTrue(emptyList.getOrganizationsList().isEmpty());

        var notEmptyList = organizationApplication.listCompanyTrees(QueryCompanyTreeDto.newBuilder()
                .setOrgId(createdCompany.getId())
                .addAllLimits(List.of(createdCompany.getId()))
                .build());
        Assertions.assertEquals(2,notEmptyList.getOrganizationsList().size());

    }

    @Test
    void testPageSearchOrganizations(){
        OrganizationDto createdCompany = organizationApplication.createTopOrganization(randomOrganization());
        PageOrganizationDto pageOrganizationDto = organizationApplication.pageSearchOrganizations(
                PageQueryDto.newBuilder()
                        .setPage(0)
                        .setPageSize(10)
                        .setOrgId(createdCompany.getId())
                        .build()
        );
        Assertions.assertFalse(pageOrganizationDto.getOrganizationsList().isEmpty());

        //query by search
        pageOrganizationDto = organizationApplication.pageSearchOrganizations(
                PageQueryDto.newBuilder()
                        .setPage(0)
                        .setPageSize(10)
                        .setOrgId(createdCompany.getId())
                        .setSearch(randomId())
                        .build()
        );
        Assertions.assertTrue(pageOrganizationDto.getOrganizationsList().isEmpty());

        pageOrganizationDto = organizationApplication.pageSearchOrganizations(
                PageQueryDto.newBuilder()
                        .setPage(0)
                        .setPageSize(10)
                        .setOrgId(createdCompany.getId())
                        .setSearch(createdCompany.getName())
                        .build()
        );
        Assertions.assertFalse(pageOrganizationDto.getOrganizationsList().isEmpty());

        //query with limits
        pageOrganizationDto = organizationApplication.pageSearchOrganizations(
                PageQueryDto.newBuilder()
                        .setPage(0)
                        .setPageSize(10)
                        .setOrgId(createdCompany.getId())
                        .setSearch(createdCompany.getName())
                        .addAllLimits(List.of(randomLong()))
                        .build()
        );
        Assertions.assertTrue(pageOrganizationDto.getOrganizationsList().isEmpty());

        var subDepartment = organizationApplication.createDepartment(randomDepartment(createdCompany));
        pageOrganizationDto = organizationApplication.pageSearchOrganizations(
                PageQueryDto.newBuilder()
                        .setPage(0)
                        .setPageSize(10)
                        .setOrgId(createdCompany.getId())
                        .setSearch(subDepartment.getName())
                        .addAllLimits(List.of(createdCompany.getId()))
                        .build()
        );
        Assertions.assertFalse(pageOrganizationDto.getOrganizationsList().isEmpty());
    }

    @Test
    void testQueryDepartmentsByEmployeeAndOrg(){
        var createdTopOrganization = organizationApplication.createTopOrganization(randomOrganization());
        var createdDepartment = organizationApplication.createDepartment(randomDepartment(createdTopOrganization));
        organizationApplication.createDepartment(randomDepartment(createdDepartment));

        var createdEmployee = employeeApplication.createEmployee(randomEmployee(createdTopOrganization.getId()));

        var emptyQuery = organizationApplication.queryDepartmentsByEmployeeAndOrg(
                QueryDepartmentsDto.newBuilder()
                        .setEmployeeId(createdEmployee.getId())
                        .setOrgId(createdTopOrganization.getId())
                        .setIncludeSubOrg(false)
                        .build()
        );
        Assertions.assertTrue(emptyQuery.getOrganizationsList().isEmpty());

        employeeApplication.assignEmployeeToOrganization(
                AssignEmployeeToOrganizationDto.newBuilder()
                        .setEmployeeId(createdEmployee.getId())
                        .setOrgId(createdDepartment.getId())
                .build());

        var query = organizationApplication.queryDepartmentsByEmployeeAndOrg(
                QueryDepartmentsDto.newBuilder()
                        .setEmployeeId(createdEmployee.getId())
                        .setOrgId(createdTopOrganization.getId())
                        .setIncludeSubOrg(false)
                        .build()
        );
        Assertions.assertEquals(1,query.getOrganizationsList().size());

        query = organizationApplication.queryDepartmentsByEmployeeAndOrg(
                QueryDepartmentsDto.newBuilder()
                        .setEmployeeId(createdEmployee.getId())
                        .setOrgId(createdTopOrganization.getId())
                        .setIncludeSubOrg(true)
                        .build()
        );
        Assertions.assertEquals(2,query.getOrganizationsList().size());
    }

    @Test
    void testPageQuerySubOrganizations(){
        OrganizationDto created = organizationApplication.createTopOrganization(randomOrganization());

        PageOrganizationDto pageOrganizationDto = organizationApplication.pageQueryOrganizations(
                PageQueryDto.newBuilder()
                        .setPage(0)
                        .setPageSize(10)
                        .setOrgId(created.getId())
                        .build()
        );
        Assertions.assertTrue(pageOrganizationDto.getOrganizationsList().isEmpty());

        organizationApplication.createDepartment(randomDepartment(created));
        pageOrganizationDto = organizationApplication.pageQueryOrganizations(
                PageQueryDto.newBuilder()
                        .setPage(0)
                        .setPageSize(10)
                        .setOrgId(created.getId())
                        .build()
        );
        Assertions.assertFalse(pageOrganizationDto.getOrganizationsList().isEmpty());
    }

    @Test
    void testQueryOpenApiEmployeeDto(){
        Assertions.assertThrowsExactly(OrganizationNotExistsException.class,()->organizationApplication.queryOpenApiEmployeeDto(Int64Value.of(randomLong())));

        var createdOrganization = organizationApplication.createTopOrganization(randomOrganization());
        var openApiEmployee = organizationApplication.queryOpenApiEmployeeDto(Int64Value.of(createdOrganization.getId()));

        Assertions.assertNotNull(openApiEmployee);
    }

    private EmployeeDto randomCreateEmployee(){
        var randomUserDto = randomUserDto();
        Mockito.when(userApplication.createLocalUser(any())).thenReturn(randomUserDto);
        Mockito.when(userApplication.queryUserById(any())).thenReturn(randomUserDto);

        var createdOrganization = organizationApplication.createTopOrganization(randomOrganization());
        EmployeeDto created = employeeApplication.createEmployee(randomEmployee(createdOrganization.getId()));
        Assertions.assertNotNull(created);
        return created;
    }
}
