/*
	#1
*/
select ee.EmpName as name, g.gradYear as gradYear
from (
		select e.EmpId as EmpId, count(*) as num
		from Employee e, EmpProject ep
		where e.EmpId = ep.EmpId
		group by e.EmpId
	) e, Graduate g, Employee ee
where e.EmpId = g.EmpId and e.num = 1 and e.EmpId = ee.EmpId;

/*
	#2
*/
select p.ProjName as name
from (
		select ep.ProjId as ProjId, count(distinct g.UnivId) as num
		from EmpProject ep, Graduate g
		where ep.EmpId = g.EmpId
		group by ep.ProjId
	) ep, Project p
where ep.ProjId = p.ProjId and ep.num = 1;

/*
	#3
*/
select p.ProjId as ProjId, count(ep.StartDate) - count(ep.EndDate) as num
from Project p left join EmpProject ep
on p.ProjId = ep.ProjId
group by p.ProjId;

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

/*
	#8
*/

select ep.ProjId as pid, ep.EmpId as eid, ep.EndDate as date1, pm.StartDate as date2
from ProjectManager pm, EmpProject ep
where ep.ProjId = pm.ProjId and ep.EmpId = pm.mgrid and pm.EndDate is null and ep.EndDate is not null and ep.startdate < pm.StartDate;