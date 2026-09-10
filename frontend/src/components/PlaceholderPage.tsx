import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '../components/ui/Card';
import { Layers } from 'lucide-react';

interface PlaceholderProps {
  title: string;
  description: string;
  phase: string;
}

export function PlaceholderPage({ title, description, phase }: PlaceholderProps) {
  return (
    <div className="max-w-3xl mx-auto py-8">
      <Card>
        <CardHeader>
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-400 flex items-center justify-center">
              <Layers className="w-5 h-5" />
            </div>
            <div>
              <CardTitle>{title}</CardTitle>
              <CardDescription>{description}</CardDescription>
            </div>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="p-4 rounded-xl bg-slate-50 dark:bg-slate-800/50 border border-slate-200/80 dark:border-slate-800">
            <p className="text-xs font-semibold uppercase tracking-wider text-indigo-600 dark:text-indigo-400">
              Scheduled Roadmap: {phase}
            </p>
            <p className="text-sm text-slate-700 dark:text-slate-300 mt-1">
              The backend data schemas and migration tables are prepared. This UI module will be actively wired in the next development phase in strict compliance with the architectural specification.
            </p>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
