import { useEffect, useRef } from "react";

export default function useInterval(callback: ()=>void, delay: number) {
    const savedCallback = useRef<()=>void>()

    useEffect(() => {
        savedCallback.current = callback;
    }, [callback]);

    let id: NodeJS.Timer 

    useEffect(()=> {
        // savedCallback.current!!() //call right away
        function tick() {
            savedCallback.current!!();
        }
        id = setInterval(tick, delay);
        return () => clearInterval(id)
    }, [callback, delay])

    return () => clearInterval(id); 
}