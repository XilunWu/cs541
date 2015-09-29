

/*
	#4
*/
select p.ProjId as ProjId, count(distinct pm.ProjId) as num
from Project p, ProjectManager pm
where p.ProjId = pm.MgrId
group by p.ProjId;

select max(pm.num) as num
from (
		select p.ProjId as ProjId, count(distinct pm.ProjId) as num
		from Project p, ProjectManager pm
		where p.ProjId = pm.MgrId
		group by p.ProjId
	) pm;
/*	it returns the max num */

/*
	#5
similarly
*/

/*	
	#6
*/
select EmpId, count(distinct ProjId) as num, avg(case when EndDate is null then CURRENT_DATE else EndDate end - StartDate + 1) as time from EmpProject group by EmpId;

/*	
	#7
*/
select pm.ProjId, max(pm.time)
from (
select pm.ProjId, pm.MgrId, sum(case when EndDate is null then CURRENT_DATE else EndDate end - StartDate + 1) as time 
from Project p, ProjectManager pm 
where p.ProjId = pm.ProjId
group by pm.ProjId, pm.MgrId
) pm
group by pm.ProjId;
/* not complete yet */