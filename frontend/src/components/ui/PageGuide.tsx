interface PageGuideProps {
  description: string;
  tips?: string[];
}

export function PageGuide({ description, tips }: PageGuideProps) {
  return (
    <details className="group bg-accent/5 border border-accent/15 rounded-lg text-[12px] text-text-secondary">
      <summary className="flex items-center gap-2 px-4 py-2.5 cursor-pointer list-none select-none hover:text-text-primary transition-colors">
        <span className="material-symbols-outlined text-[15px] text-accent shrink-0">info</span>
        <span className="flex-1">{description}</span>
        <span className="material-symbols-outlined text-[15px] text-text-muted transition-transform group-open:rotate-180">
          expand_more
        </span>
      </summary>
      {tips && tips.length > 0 && (
        <ul className="px-4 pb-3 pt-1 space-y-1 border-t border-accent/10">
          {tips.map((tip, i) => (
            <li key={i} className="flex items-start gap-2">
              <span className="text-accent mt-0.5 shrink-0">·</span>
              <span>{tip}</span>
            </li>
          ))}
        </ul>
      )}
    </details>
  );
}
