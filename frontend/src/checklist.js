export const checklist = [
  ['hardHat','Hard hat'], ['highVisibilityVest','High visibility vest'], ['safetyBoots','Safety boots'],
  ['eyeProtection','Eye protection'], ['fallProtection','Fall protection'], ['laddersScaffolds','Ladders and scaffolds'],
  ['toolsCords','Tools and cords'], ['hazardsControlled','Hazards controlled']
];
export const answers = [['PASS','Pass'],['ISSUE','Issue'],['NA','Not applicable']];
export function today(date = new Date()) {
  const parts = new Intl.DateTimeFormat('en-CA',{timeZone:'America/Vancouver',year:'numeric',month:'2-digit',day:'2-digit'}).formatToParts(date);
  const values=Object.fromEntries(parts.map(part=>[part.type,part.value]));
  return `${values.year}-${values.month}-${values.day}`;
}
