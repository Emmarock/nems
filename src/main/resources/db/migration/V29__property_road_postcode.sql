-- The estate addresses properties by road, not block/plot - collapse the two columns into one,
-- carrying over the existing block value (plot was mostly "Unnumbered" filler, see V22's seed
-- data). Also add an optional postcode now that Nigeria's federal postcode rollout exists for
-- properties to record one.
alter table property rename column block to road;
alter table property drop column plot;
alter table property add column postcode varchar(16);
