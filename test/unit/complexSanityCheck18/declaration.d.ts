declare function module(options: Editor): Editor;

interface Editor {
    on(eventName: string, handler: (instance: Editor) => void ): void;
    on(eventName: 'change', handler: (instance: Editor, change: string) => void ): void;
}