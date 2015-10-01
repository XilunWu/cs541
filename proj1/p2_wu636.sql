set serveroutput on
@db.sql

create or replace procedure sp_univ_emp 
IS
	c_univid University.UnivId%type;
	c_univname University.UnivName%type;
	NumberOfEmployees INTEGER := 0;
	AvgProjects FLOAT := 0;
	NumberOfProjects INTEGER := 0;
	NumberOfManagers INTEGER := 0;
	CURSOR c_univ is select * from University;
	
	procedure numOfEmp (univid IN University.UnivId%type,
					num OUT INTEGER)
	IS
		BEGIN		
		num := 0;
			select count(*) into num from Employee, Graduate
			where Graduate.UnivId = univid and Graduate.EmpId = Employee.EmpId;
		EXCEPTION 
        WHEN NO_DATA_FOUND THEN
          	dbms_output.put_line('No data found');
            num := 0;
		END;

	procedure numOfProj (univid IN University.UnivId%type,
					num OUT INTEGER)
	IS
		tmp INTEGER;
		BEGIN
			select count(*) into tmp from EmpProject ep, Graduate g
			where g.UnivId = univid and g.EmpId = ep.EmpId;		
			IF tmp = 0 THEN
           		num := 0;
           	ELSE
           		num := 1;
           	END IF;
		END;

	procedure numOfMgr (univid IN University.UnivId%type,
					num OUT INTEGER)
	IS
		BEGIN
			select count(distinct ProjectManager.MgrId) into num from ProjectManager, Graduate
			where Graduate.EmpId = ProjectManager.MgrId and Graduate.UnivId = univid;
		EXCEPTION 
        	WHEN NO_DATA_FOUND THEN
       	    num := 0;
		END;
		
BEGIN
	--dbms_output.put_line('start');
	dbms_output.put_line('UniversityName  NumberOfEmployees  AvgProjects   NumberOfManagers');
	dbms_output.put_line('-------------------------------------------------------------------------------------');
	OPEN c_univ;
	LOOP
		FETCH c_univ into c_univid, c_univname;
		EXIT WHEN c_univ%notfound;
		
		numOfEmp(c_univid, NumberOfEmployees);
		numOfProj(c_univid, NumberOfProjects);
		IF NumberOfProjects = 0 THEN
			NULL;
		ELSE
			select sum(table1.num) into NumberOfProjects from (
				select ep.EmpId, count(distinct ep.ProjId) as num
				from EmpProject ep, Graduate g, University u
				where u.UnivId = c_univid and u.UnivId = g.UnivId and g.EmpId = ep.EmpId
				group by ep.EmpId
			) table1;
		END IF;

		IF NumberOfEmployees = 0 THEN
			AvgProjects := 0;
		ELSE
			AvgProjects := (1.0 * NumberOfProjects) / NumberOfEmployees;
		END IF;
		
		numOfMgr(c_univid, NumberOfManagers);
		dbms_output.put_line(c_univname || chr(9) || NumberOfEmployees || chr(9) || AvgProjects || chr(9) || NumberOfManagers);
	END LOOP;
	CLOSE c_univ;
END;
/
show errors;
execute sp_univ_emp;

/*
	#2
*/
set serveroutput on

create or replace procedure sp_emp_mate 
is
	c_EmpId Employee.EmpId%type;
	c_EmpName Employee.EmpName%type;
	c_DeptId Employee.DeptId%type;
	c_ZipCode Employee.HomeZipCode%type;
	gradYear Graduate.GradYear%type;
	deptName Department.DeptName%type;
	univName University.UnivName%type;
	CURSOR c_emp IS select * from Employee;
	emp_id Employee.EmpId%type;
	emp_name Employee.EmpName%type;
	i number := 0;
BEGIN
	OPEN c_emp;
	LOOP
		BEGIN
			FETCH c_emp into c_EmpId, c_EmpName, c_DeptId, c_ZipCode;
			EXIT WHEN c_emp%notfound;
			select g.gradYear into gradYear from Graduate g where g.EmpId = c_EmpId;
			select d.DeptName into deptName from Department d where d.DeptId = c_DeptId;
			dbms_output.put_line(chr(10) || 
							 	 'Employee ID: ' || c_EmpId || chr(10) || 
								 'Employee Name: ' || c_EmpName || chr(10) || 
								 'Department: ' || deptName || chr(10) ||
								 'Graduated: ' || gradYear || chr(10));
			dbms_output.put_line('EmpoyeeId  EmployeeName  NumOfProjects   UniversityName');
			dbms_output.put_line('-------------------------------------------------------------------------------------');
			i := 0;
			FOR rec in (select EmpId, count(distinct ProjId) as num from (
							select a.ProjId as ProjId, b.EmpId as EmpId
							from EmpProject a, EmpProject b
							where a.EmpId = c_EmpId and b.ProjId = a.ProjId and b.EmpId != c_EmpId
						)
						group by EmpId
						order by num desc)
			LOOP
				i := i + 1;
				IF i > 2 THEN
					EXIT;
				END IF;
				emp_id := rec.EmpId;
				select u.UnivName into univName from University u, Graduate g where u.UnivId = g.UnivId and g.EmpId = emp_id;
				select EmpName into emp_name from Employee where EmpId = emp_id;
				dbms_output.put_line(emp_id || chr(9) || 
									 emp_name || chr(9) ||
									 rec.num || chr(9) ||
									 univName
									 );
			END LOOP;
		END;
	END LOOP;
	CLOSE c_emp;
END;
/
show errors;
exec sp_emp_mate;

/*
	#3
*/

@droptables.sql