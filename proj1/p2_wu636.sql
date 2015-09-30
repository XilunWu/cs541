set serveroutput on

create or replace procedure sp_univ_emp 
is
	c_univid University.UnivId%type;
	c_univname University.UnivName%type;
	NumberOfEmployees INTEGER := 0;
	AvgProjects numeric(2) := 0;
	NumberOfProjects INTEGER := 0;
	NumberOfManagers INTEGER := 0;
	CURSOR c_univ is select * from University;
	tmp INTEGER;
BEGIN
	--dbms_output.put_line('start');
	dbms_output.put_line('EmpoyeeId  EmployeeName  NumOfProjects   UniversityName');
	dbms_output.put_line('UniversityName' || chr(9) || 'NumberOfEmployees' || chr(9) || 'AvgProjects' || chr(9) || 'NumberOfManagers');
	dbms_output.put_line('-------------------------------------------------------------------------------------');
	OPEN c_univ;
	LOOP
		BEGIN
		FETCH c_univ into c_univid, c_univname;
		EXIT WHEN c_univ%notfound;
		--BEGIN
		select count(*) into NumberOfEmployees from Employee, Graduate
		where Graduate.EmpId = Employee.EmpId and Graduate.UnivId = c_univid;
		/*
		EXCEPTION 
        WHEN NO_DATA_FOUND THEN
          	dbms_output.put_line('No data found');
            NumberOfEmployees := 0;
		END;
		*/
		select sum(table1.num) into NumberOfProjects from (
			select ep.EmpId, count(distinct ep.ProjId) as num
			from EmpProject ep, Graduate g, University u
			where u.UnivId = c_univid and u.UnivId = g.UnivId and g.EmpId = ep.EmpId
			group by ep.EmpId
		) table1;
		/*
		BEGIN
			select count(distinct ep.ProjId) into tmp
			from EmpProject ep, Graduate g, University u
			where u.UnivId = c_univid and u.UnivId = g.UnivId and g.EmpId = ep.EmpId
			group by ep.EmpId;
		EXCEPTION 
        	WHEN NO_DATA_FOUND THEN
        		dbms_output.put_line('No data found');
           	 	NumberOfProjects := 0;
		END;
		*/

		IF NumberOfEmployees = 0 THEN
			AvgProjects := 0;
		ELSE
			AvgProjects := NumberOfProjects / NumberOfEmployees;
		END IF;
		
		--BEGIN
		select count(distinct ProjectManager.MgrId) into NumberOfManagers from ProjectManager, Graduate
		where Graduate.EmpId = ProjectManager.MgrId and Graduate.UnivId = c_univid;
		/*
		EXCEPTION 
        	WHEN NO_DATA_FOUND THEN
       	    NumberOfManagers := 0;
		END;
		*/
		dbms_output.put_line(c_univname || chr(9) || chr(9) || NumberOfEmployees || chr(9) || chr(9) || AvgProjects || chr(9) || chr(9) || NumberOfManagers);
		END;
	END LOOP;
	CLOSE c_univ;
END;
/
show errors;
execute sp_univ_emp;


/*
	#2
*/

