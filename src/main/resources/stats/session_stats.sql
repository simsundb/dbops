SELECT coorname,count(1)
FROM pgxc_stat_activity  c
GROUP BY c.coorname 
ORDER BY coorname;